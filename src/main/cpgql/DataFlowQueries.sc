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
    println("Starting top 5 longest data-flows by nodes visited...")
    var t1 = System.nanoTime
    var r1 = topDataFlows()
    t1 = System.nanoTime - t1
    println(s"Data-flows considered: ${r1.size}")
    println(s"Max: ${r1.map(_._3).maxOption}")
    println(s"Min: ${r1.map(_._3).minOption}")
    println(s"Mean: ${r1.map(_._3).sum / r1.size.asInstanceOf[Double]}")
    println(s"Finished in: $t1 ns")
    println("Starting top 5 longest data-flows by methods visited...")
    var t2 = System.nanoTime
    var r2 = longMethodDataFlows()
    t2 = System.nanoTime - t2
    println(s"Data-flows considered: ${r2.size}")
    println(s"Max: ${r2.map(_._1).maxOption}")
    println(s"Min: ${r2.map(_._1).minOption}")
    println(s"Mean: ${r2.map(_._1).sum / r2.size.asInstanceOf[Double]}")
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
 * Find top 5 longest data flows starting from a parameter and ending at a
 * method call. This returns a 4-tuple of the start method ID, end method ID,
 * length of the path and number of methods visited.
 * 
 * @return (ID(METHOD_PARAMETER_IN), ID(CALL), LENGTH(PATH), LENGTH(UNIQUE(METHODS))
 */ 
def topDataFlows(): List[(Long, Long, Int, Int)] = {
    def sinks = cpg.call
      .filterNot(x => { x.name.contains("<operator>") || !x.callee.l.exists(_.isExternal) } )
      .l
    def sources = cpg.method
      .filterNot(_.method.isExternal)
      .parameter
      .filter(_.typ.l.exists { x => x.fullName == "java.lang.String"})

    sinks.flatMap(sink =>
          sink.reachableByFlows(sources)
            .groupBy(flow => flow.elements.last)
            .sortBy({case (_, ps) => ps.map(p => p.elements.size).l.reduceOption(_ max _)})
            .lastOption
    )
    .flatMap(_._2)
    .sortBy(flow => flow.elements.size)
    .map(flow => {
        val p = flow.elements.head
        val c = flow.elements.last

        (p.id, c.id, flow.elements.size, flow.elements.map(_.method).dedup.l.size)
    })
}.l

/**
 * Find the top 5 data flows with the largest number of unique methods visited.
 * 
 * @return (LENGTH(UNIQUE(METHODS)), List(METHOD_FULL_NAMES))
 */ 
def longMethodDataFlows(): List[(Int, List[String])] = {
    def sinks = cpg.call
      .filterNot(x => { x.name.contains("<operator>") || !x.callee.l.exists(_.isExternal) } )
      .l
    def sources = cpg.method
      .filterNot(_.method.isExternal)
      .parameter
      .filter(_.typ.l.exists { x => x.fullName == "java.lang.String"})

    sinks.flatMap(sink =>
          sink.reachableByFlows(sources)
            .map(_.elements.map(_.method.fullName))
            .l
    )
    .map(_.distinct).distinct
    .sortBy(flow => flow.size)
    .map(f => (f.l.size, f.l))
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
