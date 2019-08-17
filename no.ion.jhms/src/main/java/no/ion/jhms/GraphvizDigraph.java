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
                append(" [ style=bold; label=")
                        .appendModuleNodeLabelValue("<b><u>" + htmlEscape(hybridModule.id()) + "</u></b>", exports)
                        .appendLine("; ]");
            } else {
                if (exports.size() > 0) {
                    append(" [ label=")
                            .appendModuleNodeLabelValue(htmlEscape(hybridModule.id()), exports)
                            .appendLine("; ]");
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
                    append(" [ label=")
                            .appendModuleNodeLabelValue(htmlEscape(module.name()), exports)
                            .appendLine("; ]");
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

    private static String htmlEscape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    private GraphvizDigraph appendModuleNodeLabelValue(String moduleHtml, List<String> unqualifiedExports) {
        if (unqualifiedExports.size() > 0) {
            append("<<table border=\"0\"><tr><td>")
                    .appendHtmlEscaped(moduleHtml)
                    .append("</td></tr><tr><td><i><font color=\"dimgray\">")
                    .appendHtmlEscaped(unqualifiedExports.get(0));
            for (int i = 1; i < unqualifiedExports.size(); ++i) {
                append("<br/>");
                appendHtmlEscaped(unqualifiedExports.get(i));
            }
            append("</font></i></td></tr></table>>");
        } else {
            append("<").append(moduleHtml).append(">");
        }
        return this;
    }

    private GraphvizDigraph appendExportsTable(List<String> unqualifiedExports) {
        if (unqualifiedExports.size() <= 0) {
            throw new IllegalArgumentException("unqualifiedExports must have a positive size");
        }

        // The distance between two characters horizontally (H) and vertically (V) has a physical distance
        // (and visual ratio) of ... say V/H = 3. To get a circle, we need something like
        //   V * rows = H * (max # characters in column) * columns
        // On the other hand, rows * columns ~ unqualifiedExports.size(), therefore
        //   rows * columns * H * (max # characters in column) * columns = unqualifiedExports.size() * V * rows
        //   columns^2 = unqualifiedExports.size() * V / (H * (max # characters in column))
        //   columns = sqrt( unqualifiedExports.size() * (V/H) / (max # characters in column) )

        // V/H
        double ratio = 3.0;

        // max # characters in columns
        int maxLength = unqualifiedExports.stream().map(String::length).max(Integer::compareTo).orElse(0);
        if (maxLength <= 0) {
            throw new IllegalArgumentException("Only empty package names");
        }

        int columns = (int) Math.floor(Math.sqrt(unqualifiedExports.size() * ratio / maxLength));
        if (columns <= 0) {
            columns = 1;
        }

        append("<i><table border=\"0\">");

        for (int row = 0; ; ++row) {
            int offset = columns * row;
            if (offset >= unqualifiedExports.size()) {
                break;
            }

            append("<tr");

            for (int column = 0; column < columns; ++column) {
                int index = offset + column;
                String content = index < unqualifiedExports.size() ? unqualifiedExports.get(index) : "";
                append("<td>").appendHtmlEscaped(content).append("</td>");
            }

            append("</tr>");
        }

        append("</table></i>");

        return this;
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
        dot.append(htmlEscape(text));
        return this;
    }

    private void appendLine(String line) {
        dot.append(line).append('\n');
    }
}
