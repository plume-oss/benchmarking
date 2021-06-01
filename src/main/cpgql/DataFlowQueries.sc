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
 * @return (IDENTIFIER.name, METHOD.name, METHOD.lineNumber)
 */
({
    def assignSinks = cpg.call.filter(_.name.contains("<operator>.assignment")).l
    def otherSinks = cpg.call.filterNot(_.name.contains("<operator>.assignment")).l
    def sources = cpg.identifier

    def flowsWithAssignment = assignSinks.map(sink =>
        sink.reachableByFlows(sources)
    ).toSet

    def flowsWithoutAssignment = otherSinks.map(sink =>
        sink.reachableByFlows(sources)
    ).toSet

    flowsWithoutAssignment
        .diff(flowsWithAssignment)
        .flatten
        .map(flow => (flow.elements.head, flow.elements.head.method))
        .dedupBy(_._1)
        .map({case (n: Identifier, m: Method) => (n.name, m.name, m.lineNumber)})
}).takeRight(5).l