@main def exec(cpgFile: String, outFile: String) = {
    importCpg(cpgFile)
    val fileName = """/(\w\d).bin$""".r.findAllIn(cpgFile).matchData.map(_.group(1)).head
    val program = fileName.take(1) match {
        case "n" => "neo4j"
        case "g" => "gremlin-driver"
        case "j" => "jackson-databind"
        case _ => "poop"
    }
    val commitKey = fileName(1) match {
        case '1' => "I"
        case '2' => "0"
        case '3' => "1"
        case '4' => "2"
        case '5' => "3"
        case _ => "poop"
    }
    s"B$commitKey,${runQueries(fileName)}" |>> outFile
}

def runQueries(fileName: String): String = {
    println("=====================================================")
    println(s"Running $fileName")
    println("Starting string to external call...")
    var t1 = System.nanoTime
    var r1 = stringToExternalCall()
    t1 = System.nanoTime - t1
    println(s"Data-flows considered: ${r1.size}")
    println(s"Max: ${r1.map(_.size).maxOption}")
    println(s"Min: ${r1.map(_.size).minOption}")
    println(s"Mean: ${r1.map(_.size).sum / r1.size.asInstanceOf[Double]}")
    println(s"Finished in: $t1 ns")
    println("Starting string to any log or print...")
    var t2 = System.nanoTime
    var r2 = stringToPrintLog()
    t2 = System.nanoTime - t2
    println(s"Data-flows considered: ${r2.size}")
    println(s"Max: ${r2.map(_.size).maxOption}")
    println(s"Min: ${r2.map(_.size).minOption}")
    println(s"Mean: ${r2.map(_.size).sum / r2.size.asInstanceOf[Double]}")
    println(s"Finished in: $t2 ns")
    println("Starting simple constants detection...")
    var t3 = System.nanoTime
    var r3 = simpleConstants()
    t3 = System.nanoTime - t3
    println(s"Result: ${r3.size}")
    println(s"Finished in: $t3 ns")
    println("=====================================================")
    s"$fileName,$t1,$t2,$t3"
}

/**
 * Find all string sources from parameters that start at application methods and
 * reach external method sinks.
 * 
 * @return list of data flows.
 */ 
def stringToExternalCall(): List[List[Node]] = {
    def sinks = cpg.call
      .filterNot(x => { x.name.contains("<operator>") || !x.callee.l.exists(_.isExternal) } )
      .l
    def sources = cpg.method
      .filterNot(_.method.isExternal)
      .parameter
      .filter(_.typ.l.exists { x => x.fullName == "java.lang.String"})

    sinks.flatMap(sink => sink.reachableByFlows(sources))
    .map(_.elements)
}.l

/**
 * Find all string sources from parameters that start at application methods and
 * hit any log or print sinks.
 * 
 * @return list of data flows.
 */ 
def stringToPrintLog(): List[List[Node]] = {
    val logR = """(?i)(log|print)""".r
    def sinks = cpg.call
      .filterNot(x => { x.name.contains("<operator>") } )
      .filter(x => { !logR.findAllIn(x.methodFullName).matchData.isEmpty} )
      .l
    def sources = cpg.method
      .filterNot(_.method.isExternal)
      .parameter
      .filter(_.typ.l.exists { x => x.fullName == "java.lang.String"})

    sinks.flatMap(sink => sink.reachableByFlows(sources))
    .map(_.elements)
}.l

/**
 * Return all identifiers which do not get re-assigned and can thus be a
 * candidate for constant propagation.
 *
 * @return List[Identifier] of identifiers of primitive types where all
 * occurrences can be replaced by the value in their initial declaration.
 */
def simpleConstants(): List[Identifier] = {
  import io.shiftleft.semanticcpg.language.operatorextension.opnodes.Assignment

   cpg.assignment
    .groupBy(_.argument.order(1).code.l)
    .flatMap {
      case (_: List[String], as: Traversal[Assignment]) => Option(as.l)
      case _ => Option.empty
    }.filter(_.size == 1)
    .flatMap {
      case as: List[Assignment] =>
        Option(as.head.argument.head, as.head.argument.l.head.typ.l)
      case _ => Option.empty
    }
    // Filter only primitives
    .filter {
      case (_: Identifier, ts: List[Type]) =>
        ts.nonEmpty &&
          ts.head.namespace.l.exists { x => x.name.contains("<global>")} &&
          !ts.head.fullName.contains("[]")
      case _ => false
    }
    .flatMap {
      case (i: Identifier, _: List[Type]) => Option(i)
      case _ => Option.empty
    }
}.l
