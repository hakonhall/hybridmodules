package no.ion.jhms;

import java.util.List;

class GraphvizDigraph {
    private final String name;
    private final ModuleGraph graph;

    private final StringBuilder dot = new StringBuilder();

    static GraphvizDigraph fromModuleGraph(ModuleGraph graph) {
        return new GraphvizDigraph(graph);
    }

    private GraphvizDigraph(ModuleGraph graph) {
        this.name = "module graph";
        this.graph = graph;
    }

    String toDot() {
        dot.setLength(0);

        append("digraph ").appendId(name).append(" {").appendLine();
        appendLine("  subgraph cluster_hybrid {");
        appendLine("    graph [ style=dotted; label=<<font face=\"Helvetica\">HYBRID MODULES</font>>; ]");
        appendLine("    node [ color=blue; ]");
        graph.hybridModules().forEach(hybridModule -> {
            append("    ").appendId(hybridModule.id());

            List<String> exports = hybridModule.unqualifiedExports();
            if (graph.rootHybridModules().contains(hybridModule.id())) {
                if (exports.size() > 0) {
                    append(" [ style=bold; label=<<table border=\"0\"><tr><td><b><u>")
                            .appendHtmlEscaped(hybridModule.id())
                            .append("</u></b></td></tr><tr><td><i><font color=\"dimgray\">")
                            .appendHtmlEscaped(exports.get(0));
                    for (int i = 1; i < exports.size(); ++i) {
                        append("<br/>");
                        appendHtmlEscaped(exports.get(i));
                    }
                    appendLine("</font></i></td></tr></table>>; ]");
                } else {
                    append(" [ style=bold; label=<<b><u>").appendHtmlEscaped(hybridModule.id()).appendLine("</u></b>>; ]");
                }
            } else {
                if (exports.size() > 0) {
                    append(" [ label=<<table border=\"0\"><tr><td>")
                            .appendHtmlEscaped(hybridModule.id())
                            .append("</td></tr><tr><td><i><font color=\"dimgray\">")
                            .appendHtmlEscaped(exports.get(0));
                    for (int i = 1; i < exports.size(); ++i) {
                        append("<br/>");
                        appendHtmlEscaped(exports.get(i));
                    }
                    appendLine("</font></i></td></tr></table>>; ]");
                } else {
                    appendLine();
                }
            }
        });
        appendLine("  }");
        if (graph.platformModules().size() > 0) {
            appendLine("  subgraph cluster_platform {");
            appendLine("    graph [ style=dotted; label=<<font face=\"Helvetica\">PLATFORM MODULES</font>>; ]");
            appendLine("    node [ color=red; ]");
            graph.platformModules().forEach(module -> {
                append("    ").appendId(module.name());
                List<String> exports = module.unqualifiedExports();
                if (exports.size() > 0) {
                    append(" [ label=<<table border=\"0\"><tr><td>").appendHtmlEscaped(module.name()).append("</td></tr><tr><td><i><font color=\"dimgray\">");

                    appendHtmlEscaped(exports.get(0));

                    for (int i = 1; i < exports.size(); ++i) {
                        append("<br/>").appendHtmlEscaped(exports.get(i));
                    }

                    appendLine("</font></i></td></tr></table>>; ]");
                } else {
                    appendLine();
                }
            });
            appendLine("  }");
        }
        graph.readEdges()
                .forEach((from, toList) -> toList
                        .forEach(toEdge -> {
                            append("  ").appendId(from).append(" -> ").appendId(toEdge.toModule());

                            List<String> exports = toEdge.exports();
                            if (exports.size() > 0) {
                                append(" [ label=<<i><font color=\"dimgray\">").appendHtmlEscaped(exports.get(0));

                                for (int i = 1; i < exports.size(); ++i) {
                                    append("<br/>").appendHtmlEscaped(exports.get(i));
                                }

                                appendLine("</font></i>>; ]");
                            } else {
                                appendLine();
                            }
                        }));
        appendLine("}");

        return dot.toString();
    }

    private void appendLine() {
        dot.append('\n');
    }

    private GraphvizDigraph append(String string) {
        dot.append(string);
        return this;
    }

    private GraphvizDigraph appendDoubleQuoteEscaped(String id) {
        if (id.indexOf('<') != -1) {
            throw new IllegalArgumentException("Appending an HTML ID is not supported: " + id);
        }

        dot.append(id.replace("\"", "\\\""));
        return this;
    }

    /** Note: id cannot be HTML. */
    private GraphvizDigraph appendId(String id) {
        dot.append('"');
        appendDoubleQuoteEscaped(id);
        dot.append('"');
        return this;
    }

    private GraphvizDigraph appendHtmlEscaped(String text) {
        dot.append(text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;"));
        return this;
    }

    private void appendLine(String line) {
        dot.append(line).append('\n');
    }
}
