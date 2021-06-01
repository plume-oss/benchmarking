@main def exec(cpgFile: String, outFile: String) = {
    loadCpg(cpgFile)
    cpg.method.name.l |> outFile
}


/**
 * Find top 5 longest data flows starting from a parameter and ending at a
 * method call. This returns a 4-tuple of the start method ID, end method ID,
 * length of the path and number of methods visited.
 * 
 * @return (ID(METHOD_PARAMETER_IN), ID(CALL), LENGTH(PATH), LENGTH(UNIQUE(METHODS))
 */ 
({
    def sinks = cpg.call.filterNot(_.name.contains("<operator>")).l
    def sources = cpg.method.parameter

    sinks.flatMap(sink =>
        sink.reachableByFlows(sources)
            .groupBy(flow => flow.elements.last)
            .sortBy({case (_, ps) => ps.map(p => p.elements.size).l.reduceOption(_ max _)})
            .lastOption
    ).flatMap(_._2)
    .sortBy(flow => flow.elements.size)
    .map(flow => {
        val p = flow.elements.head
        val c = flow.elements.last

        (p.id, c.id, flow.elements.size, flow.elements.map(_.method).dedup.l.size)
    })
}).takeRight(5).l

/**
 * Find the top 5 data flows with the largest number of unique methods visited.
 * 
 * @return (LENGTH(UNIQUE(METHODS), List(METHOD_FULL_NAMES))
 */ 
({
    def sinks = cpg.call.filterNot(_.name.contains("<operator>")).l
    def sources = cpg.method.parameter

    sinks.flatMap(sink =>
        sink.reachableByFlows(sources)
            .map(_.elements.map(_.method.fullName))
            .l
    )
    .map(_.distinct).distinct
    .sortBy(flow => flow.size)
    .map(f => (f.l.size, f.l))
}).takeRight(5).l

/**
 * Return all identifiers which do not get re-assigned and can thus be a
 * candidate for constant propagation.
 *
 * @return List[Identifier] of identifiers of primitive types where all
 * occurrences can be replaced by the value in their initial declaration.
 */
({ 
    import io.shiftleft.semanticcpg.language.operatorextension.opnodes.Assignment

    cpg.assignment
    .groupBy(_.argument.order(1).code.l)
    .map { case (_, as: Traversal[Assignment]) => as.l }
    .filter(_.size == 1)
    .flatMap { 
        case as: List[Assignment] => Option(as.head.argument.head, as.head.argument.l(0).typ.l)
        case _ => Option.empty
    }
    .filter { case (_: Identifier, ts: List[Type]) => 
       ts.nonEmpty && ts.head.namespace.l.exists { x => x.name.contains("<global>") } && !ts.head.fullName.contains("[]")
    }
    .map { case (i: Identifier, _: List[Type]) => i }
}).l

importCode.c.fromString("""
       void main()
       {
         int a = 1;
         int b = 1 + 3;
         int c = 3;
         c = 5;
         a = 2;
       }""") 