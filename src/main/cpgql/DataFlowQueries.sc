@main def exec(cpgFile: String, outFile: String) = {
    importCpg(cpgFile)
    val fileName = """/(\w\d).bin$""".r.findAllIn(cpgFile).matchData.map(_.group(1)).head
    val program = fileName.take(1) match {
        case "n" => "neo4j"
        case "g" => "gremlin-driver"
        case "j" => "jackson-databind"
        case _ => "poop"
    }
    val whiteListKey = fileName(1) match {
        case '1' => "I"
        case '2' => "0"
        case '3' => "1"
        case '4' => "2"
        case '5' => "3"
        case _ => "poop"
    }
    val whiteList = scala.collection.mutable.Set[String]()
    scala.util.Using.resource(new java.io.BufferedReader(new java.io.FileReader(s"./results/changed_methods_$program.csv"))) { f =>
      var line = f.readLine
      while (line != null) {
        val tup = line.split(",")
        if (tup(0) == whiteListKey)
          whiteList.add(tup(1))
        line = f.readLine
      }
    }

    // s"U$whiteListKey,${runQueries(fileName, whiteList, true)}" |>> outFile
    s"B$whiteListKey,${runQueries(fileName, whiteList, false)}" |>> outFile
}

def runQueries(fileName: String, whiteList: scala.collection.mutable.Set[String], useWhiteList: Boolean): String = {
    print("Starting top 5 longest data-flows by nodes visited...")
    var t1 = System.nanoTime
    topDataFlows(whiteList, useWhiteList)
    t1 = System.nanoTime - t1
    println(s"Finished in: $t1 ns")
    print("Starting top 5 longest data-flows by methods visited...")
    var t2 = System.nanoTime
    longMethodDataFlows(whiteList, useWhiteList)
    t2 = System.nanoTime - t2
    println(s"Finished in: $t2 ns")
    print("Starting simple constants detection...")
    var t3 = System.nanoTime
    simpleConstants(whiteList, useWhiteList)
    t3 = System.nanoTime - t3
    println(s"Finished in: $t3 ns")
    s"$fileName,$t1,$t2,$t3"
}

// TODO: dataflow queries need to check if a whitelisted method is in the middle
// of the flow

/**
 * Find top 5 longest data flows starting from a parameter and ending at a
 * method call. This returns a 4-tuple of the start method ID, end method ID,
 * length of the path and number of methods visited.
 * 
 * @return (ID(METHOD_PARAMETER_IN), ID(CALL), LENGTH(PATH), LENGTH(UNIQUE(METHODS))
 */ 
def topDataFlows(whiteList: scala.collection.mutable.Set[String], useWhiteList: Boolean): List[(Long, Long, Int, Int)] = {
    def sinks = cpg.call
      .filterNot(x => { x.name.contains("<operator>") || !x.callee.l.exists(_.isExternal) } )
      .l
    def sources = cpg.method
      .filterNot(_.method.isExternal)
      .parameter
      .filter(_.typ.l.exists { x => x.fullName == "java.lang.String"})

    sinks.flatMap(sink =>
        (if (useWhiteList)
          sink.reachableByFlows(sources)
            .filter(flow => { flow.elements.exists(y => { whiteList.contains(y.method.fullName) }) })
        else
          sink.reachableByFlows(sources)
        )
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
}.takeRight(5).l

/**
 * Find the top 5 data flows with the largest number of unique methods visited.
 * 
 * @return (LENGTH(UNIQUE(METHODS), List(METHOD_FULL_NAMES))
 */ 
def longMethodDataFlows(whiteList: scala.collection.mutable.Set[String], useWhiteList: Boolean): List[Any] = {
    def sinks = cpg.call
      .filterNot(x => { x.name.contains("<operator>") || !x.callee.l.exists(_.isExternal) } )
      .l
    def sources = cpg.method
      .filterNot(_.method.isExternal)
      .parameter
      .filter(_.typ.l.exists { x => x.fullName == "java.lang.String"})

    sinks.flatMap(sink =>
        (if (useWhiteList)
          sink.reachableByFlows(sources)
            .filter(flow => { flow.elements.exists(y => { whiteList.contains(y.method.fullName) }) })
        else
          sink.reachableByFlows(sources)
        )
            .map(_.elements.map(_.method.fullName))
            .l
    )
    .map(_.distinct).distinct
    .sortBy(flow => flow.size)
    .map(f => (f.l.size, f.l))
}.takeRight(5).l

/**
 * Return all identifiers which do not get re-assigned and can thus be a
 * candidate for constant propagation.
 *
 * @return List[Identifier] of identifiers of primitive types where all
 * occurrences can be replaced by the value in their initial declaration.
 */
def simpleConstants(whiteList: scala.collection.mutable.Set[String], useWhiteList: Boolean): List[Identifier] = {
  import io.shiftleft.semanticcpg.language.operatorextension.opnodes.Assignment

  (if (useWhiteList)
    cpg.assignment
      .groupBy(_.argument.order(1).code.l)
      .flatMap {
        case (_: List[String], as: Traversal[Assignment]) => Option(as.l)
        case _ => Option.empty
      }
      .filter(_.exists(x => { whiteList.contains(x.method.fullName) }))
  else
   cpg.assignment
    .groupBy(_.argument.order(1).code.l)
    .flatMap {
      case (_: List[String], as: Traversal[Assignment]) => Option(as.l)
      case _ => Option.empty
    }
  ).filter(_.size == 1)
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
