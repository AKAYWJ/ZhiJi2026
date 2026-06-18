// data/remote/dto/KnowledgeGraphResponse.java
package top.ywj.zhiji2026.data.remote.dto;

import java.util.List;

public class KnowledgeGraphResponse {
    private List<Node> nodes;
    private List<Edge> edges;

    public List<Node> getNodes() { return nodes; }
    public List<Edge> getEdges() { return edges; }

    public static class Node {
        private String id;
        private String label;
        private int group;

        public String getId() { return id; }
        public String getLabel() { return label; }
        public int getGroup() { return group; }
    }

    public static class Edge {
        private String source;
        private String target;
        private String label;

        public String getSource() { return source; }
        public String getTarget() { return target; }
        public String getLabel() { return label; }
    }
}
