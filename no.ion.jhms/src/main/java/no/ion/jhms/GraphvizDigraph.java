package no.ion.jhms;

import java.util.List;

class GraphvizDigraph {
    private static final String REQUIRES_TRANSITIVE_ARROWHEAD = "normaloinv";
    private static final String IMPLICIT_READ_EDGE_STYLE = "dashed";
    private static final String MODULE_CLUSTER_FONT_FACE = "Helvetica";
    private static final String MODULE_CLUSTER_COLOR = "khaki";
    private static final String HYBRID_MODULE_BORDER_COLOR = "blue";
    private static final String PLATFORM_MODULE_BORDER_COLOR = "red";
    private static final String PACKAGES_COLOR = "dimgray";
    private static final String PACKAGES_BORDER_COLOR = "yellowgreen";


    private final String name;
    private final ModuleGraph graph;
    private final StringBuilder dot = new StringBuilder();

    private boolean attributeListOpened = false;

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

        if (graph.platformModules().size() > 0) {
            appendLine("  subgraph cluster_hybrid {");
            appendLine("    graph [ color=" + MODULE_CLUSTER_COLOR + "; label=<<font face=\"" + MODULE_CLUSTER_FONT_FACE +
                    "\">HYBRID MODULES</font>>; ]");
        }

        appendLine("    node [ color=" + HYBRID_MODULE_BORDER_COLOR + "; ]");
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

        if (graph.platformModules().size() > 0) {
            appendLine("  }");

            appendLine("  subgraph cluster_platform {");
            appendLine("    graph [ color=" + MODULE_CLUSTER_COLOR + "; label=<<font face=\"" +
                    MODULE_CLUSTER_FONT_FACE + "\">PLATFORM MODULES</font>>; ]");
            appendLine("    node [ color=" + PLATFORM_MODULE_BORDER_COLOR + "; ]");
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

                            switch (toEdge.type()) {
                                case REQUIRES:
                                    break;
                                case REQUIRES_TRANSITIVE:
                                    openAttributeList().append(" arrowhead=" + REQUIRES_TRANSITIVE_ARROWHEAD + ";");
                                    break;
                                case IMPLICIT:
                                    openAttributeList().append(" style=" + IMPLICIT_READ_EDGE_STYLE + ";");
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unknown type " + toEdge.type());
                            }

                            List<String> exports = toEdge.exports();
                            if (exports.size() > 0) {
                                openAttributeList().append(" label=<").appendExportsTable(exports).append(">;");
                            }

                            closeAttributeList().appendLine();
                        }));
        appendLine("}");

        return dot.toString();
    }

    private GraphvizDigraph openAttributeList() {
        if (!attributeListOpened) {
            append(" [");
            attributeListOpened = true;
        }
        return this;
    }

    private GraphvizDigraph closeAttributeList() {
        if (attributeListOpened) {
            append(" ]");
            attributeListOpened = false;
        }
        return this;
    }

    private static String htmlEscape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    private GraphvizDigraph appendModuleNodeLabelValue(String moduleHtml, List<String> unqualifiedExports) {
        if (unqualifiedExports.size() > 0) {
            append("<<table border=\"0\"><tr><td>")
                    .append(moduleHtml)
                    .append("</td></tr><tr><td>")
                    .appendExportsTable(unqualifiedExports)
                    .append("</td></tr></table>>");
        } else {
            append("<").append(moduleHtml).append(">");
        }
        return this;
    }

    /** Pack a long list of packages into a 2-dimensional square-sized table. */
    private GraphvizDigraph appendExportsTable(List<String> exports) {
        if (exports.size() <= 0) {
            throw new IllegalArgumentException("exports must have a positive size");
        }

        // We will try to make a table which is about twice as wide as it is high, which is a typical shape
        // for a node of just a text string.
        final double W = 2.0;

        // Let V be the vertical height between two rows in a table on screen, and H be the horizontal distance
        // between two characters. V/H is about 3 on my screen. My OS, dot program, etc would be no good if
        // this ration is radically different on someone else's screen.
        final double vOverH = 3.0;

        // Let columnLength be the number of characters in the String that fills the width of the columns.
        // As a simplification, we'll estimate it from the longest String in 'exports'.
        final int columnLength = exports.stream().map(String::length).max(Integer::compareTo).orElse(0);
        if (columnLength <= 0) {
            throw new IllegalArgumentException("Only empty package names");
        }

        // We now got the following equation for W
        //
        //       H * columns * lengthPerColumn
        //   W = -----------------------------
        //       V * rows
        //
        // On the other hand,
        //
        //   rows * columns = exports.size()
        //
        // Therefore
        //
        //               W * V/H * exports.size()
        //   columns^2 = ------------------------
        //                   lengthPerColumn
        //
        int columns = (int) Math.round(Math.sqrt(W * exports.size() * vOverH / columnLength));
        columns = Math.max(1, Math.min(columns, exports.size()));

        // Ensure the table is large enough, i.e. rows * columns >= exports.size().
        // Note: rows is guaranteed to be >= 1 (ignoring overflows).
        int rows = (exports.size() + columns - 1) / columns;

        append("<font color=\"" + PACKAGES_COLOR + "\"><table border=\"1\" color=\"" + PACKAGES_BORDER_COLOR + "\"><tr>");

        for (int column = 0; column < columns; ++column) {
            int offset = rows * column;

            append("<td balign=\"left\" valign=\"top\" border=\"0\"><i>");

            for (int row = 0; row < rows; ++row) {
                int index = offset + row;
                if (index >= exports.size()) {
                    break;
                }

                appendHtmlEscaped(exports.get(index)).append("<br/>");
            }

            append("</i></td>");
        }

        append("</tr></table></font>");

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
