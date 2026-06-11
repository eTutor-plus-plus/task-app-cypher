package at.jku.dke.task_app.cypher.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CypherGraphPngExporter {

    private static final Color[] OKABE_ITO = {
        new Color(230, 159, 0),
        new Color(86, 180, 233),
        new Color(0, 158, 115),
        new Color(240, 228, 66),
        new Color(0, 114, 178),
        new Color(213, 94, 0),
        new Color(204, 121, 167),
        new Color(120, 120, 120)
    };

    private static final int WIDTH = 900;
    private static final int HEIGHT = 600;
    private static final int MARGIN = 60;
    private static final int LEGEND_WIDTH = 220;
    private static final int NODE_RADIUS = 26;
    private static final int FR_ITERATIONS = 250;
    private static final int MAX_LABEL_CHARS = 16;

    private CypherGraphPngExporter() {
    }

    public static BufferedImage render(GraphSnapshot snapshot, String locale, long layoutSeed) {
        if (snapshot.nodes().isEmpty())
            return null;

        Map<String, Color> labelColors = assignColors(snapshot.nodes());
        Map<String, double[]> positions = layout(snapshot, layoutSeed);

        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            drawEdges(g, snapshot, positions);
            drawNodes(g, snapshot, positions, labelColors);
            drawLegend(g, labelColors, locale);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static Map<String, Color> assignColors(List<GraphSnapshot.Node> nodes) {
        Map<String, Color> colors = new LinkedHashMap<>();
        for (GraphSnapshot.Node node : nodes) {
            String key = labelKey(node);
            if (!colors.containsKey(key))
                colors.put(key, OKABE_ITO[colors.size() % OKABE_ITO.length]);
        }
        return colors;
    }

    private static String labelKey(GraphSnapshot.Node node) {
        return node.labels().isEmpty() ? "" : String.join(":", node.labels());
    }

    private static Map<String, double[]> layout(GraphSnapshot snapshot, long seed) {
        int n = snapshot.nodes().size();
        int plotWidth = WIDTH - 2 * MARGIN - LEGEND_WIDTH;
        int plotHeight = HEIGHT - 2 * MARGIN;
        double area = (double) plotWidth * plotHeight;
        double k = Math.sqrt(area / Math.max(1, n));

        Random rng = new Random(seed);
        Map<String, double[]> pos = new HashMap<>();
        for (GraphSnapshot.Node node : snapshot.nodes()) {
            pos.put(node.id(), new double[]{rng.nextDouble() * plotWidth, rng.nextDouble() * plotHeight});
        }

        double temperature = plotWidth / 8.0;
        double cooling = temperature / FR_ITERATIONS;
        Map<String, double[]> disp = new HashMap<>();

        for (int iter = 0; iter < FR_ITERATIONS; iter++) {
            for (GraphSnapshot.Node v : snapshot.nodes())
                disp.put(v.id(), new double[]{0, 0});

            for (int i = 0; i < snapshot.nodes().size(); i++) {
                double[] pi = pos.get(snapshot.nodes().get(i).id());
                for (int j = i + 1; j < snapshot.nodes().size(); j++) {
                    double[] pj = pos.get(snapshot.nodes().get(j).id());
                    double dx = pi[0] - pj[0];
                    double dy = pi[1] - pj[1];
                    double dist = Math.max(0.01, Math.hypot(dx, dy));
                    double force = (k * k) / dist;
                    double fx = (dx / dist) * force;
                    double fy = (dy / dist) * force;
                    double[] di = disp.get(snapshot.nodes().get(i).id());
                    double[] dj = disp.get(snapshot.nodes().get(j).id());
                    di[0] += fx; di[1] += fy;
                    dj[0] -= fx; dj[1] -= fy;
                }
            }

            for (GraphSnapshot.Edge edge : snapshot.edges()) {
                double[] ps = pos.get(edge.sourceId());
                double[] pt = pos.get(edge.targetId());
                if (ps == null || pt == null) continue;
                double dx = ps[0] - pt[0];
                double dy = ps[1] - pt[1];
                double dist = Math.max(0.01, Math.hypot(dx, dy));
                double force = (dist * dist) / k;
                double fx = (dx / dist) * force;
                double fy = (dy / dist) * force;
                double[] ds = disp.get(edge.sourceId());
                double[] dt = disp.get(edge.targetId());
                ds[0] -= fx; ds[1] -= fy;
                dt[0] += fx; dt[1] += fy;
            }

            for (GraphSnapshot.Node v : snapshot.nodes()) {
                double[] p = pos.get(v.id());
                double[] d = disp.get(v.id());
                double dispLen = Math.max(0.01, Math.hypot(d[0], d[1]));
                double limited = Math.min(dispLen, temperature);
                p[0] += (d[0] / dispLen) * limited;
                p[1] += (d[1] / dispLen) * limited;
                p[0] = Math.max(0, Math.min(plotWidth, p[0]));
                p[1] = Math.max(0, Math.min(plotHeight, p[1]));
            }

            temperature = Math.max(0.5, temperature - cooling);
        }

        for (double[] p : pos.values()) {
            p[0] += MARGIN;
            p[1] += MARGIN;
        }
        return pos;
    }

    private static void drawEdges(Graphics2D g, GraphSnapshot snapshot, Map<String, double[]> positions) {
        g.setStroke(new BasicStroke(1.5f));
        Font edgeFont = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
        FontMetrics fm = g.getFontMetrics(edgeFont);

        for (GraphSnapshot.Edge edge : snapshot.edges()) {
            double[] s = positions.get(edge.sourceId());
            double[] t = positions.get(edge.targetId());
            if (s == null || t == null) continue;

            double dx = t[0] - s[0];
            double dy = t[1] - s[1];
            double dist = Math.max(0.01, Math.hypot(dx, dy));
            double ux = dx / dist;
            double uy = dy / dist;

            // Trim line to circle borders so the arrow head sits flush with the target node.
            double sx = s[0] + ux * NODE_RADIUS;
            double sy = s[1] + uy * NODE_RADIUS;
            double tx = t[0] - ux * NODE_RADIUS;
            double ty = t[1] - uy * NODE_RADIUS;

            g.setColor(new Color(120, 120, 120));
            g.draw(new Line2D.Double(sx, sy, tx, ty));
            drawArrowHead(g, tx, ty, ux, uy);

            if (edge.type() != null && !edge.type().isBlank()) {
                g.setFont(edgeFont);
                g.setColor(new Color(70, 70, 70));
                String label = edge.type();
                int textWidth = fm.stringWidth(label);
                double mx = (sx + tx) / 2.0;
                double my = (sy + ty) / 2.0;
                g.setColor(new Color(255, 255, 255, 220));
                g.fillRect((int) (mx - textWidth / 2.0) - 2, (int) (my - fm.getAscent() / 2.0) - 1, textWidth + 4, fm.getHeight());
                g.setColor(new Color(70, 70, 70));
                g.drawString(label, (int) (mx - textWidth / 2.0), (int) (my + fm.getAscent() / 2.0) - 2);
            }
        }
    }

    private static void drawArrowHead(Graphics2D g, double tipX, double tipY, double ux, double uy) {
        double headLength = 10;
        double headWidth = 6;
        double bx = tipX - ux * headLength;
        double by = tipY - uy * headLength;
        double px = -uy;
        double py = ux;
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tipX, tipY);
        head.lineTo(bx + px * headWidth, by + py * headWidth);
        head.lineTo(bx - px * headWidth, by - py * headWidth);
        head.closePath();
        g.fill(head);
    }

    private static void drawNodes(Graphics2D g, GraphSnapshot snapshot, Map<String, double[]> positions, Map<String, Color> labelColors) {
        Font nodeFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        FontMetrics fm = g.getFontMetrics(nodeFont);
        g.setStroke(new BasicStroke(1.5f));

        for (GraphSnapshot.Node node : snapshot.nodes()) {
            double[] p = positions.get(node.id());
            if (p == null) continue;
            int x = (int) Math.round(p[0]);
            int y = (int) Math.round(p[1]);

            Color fill = labelColors.getOrDefault(labelKey(node), OKABE_ITO[OKABE_ITO.length - 1]);
            g.setColor(fill);
            g.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            g.setColor(fill.darker());
            g.drawOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

            g.setFont(nodeFont);
            g.setColor(textColorOn(fill));
            String label = nodeLabel(node);
            int textWidth = fm.stringWidth(label);
            g.drawString(label, x - textWidth / 2, y + fm.getAscent() / 2 - 2);
        }
    }

    private static void drawLegend(Graphics2D g, Map<String, Color> labelColors, String locale) {
        int legendX = WIDTH - LEGEND_WIDTH + 10;
        int legendY = MARGIN;
        int line = 28;
        int swatch = 16;

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g.setColor(Color.BLACK);
        g.drawString("en".equals(locale) ? "Node labels" : "Knoten-Labels", legendX, legendY);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        int row = 0;
        for (Map.Entry<String, Color> entry : labelColors.entrySet()) {
            int y = legendY + line + row * line;
            g.setColor(entry.getValue());
            g.fillOval(legendX, y - swatch + 4, swatch, swatch);
            g.setColor(entry.getValue().darker());
            g.drawOval(legendX, y - swatch + 4, swatch, swatch);
            g.setColor(Color.BLACK);
            String key = entry.getKey().isEmpty()
                ? ("en".equals(locale) ? "(no label)" : "(ohne Label)")
                : ":" + entry.getKey();
            g.drawString(key, legendX + swatch + 8, y);
            row++;
        }
    }

    private static String nodeLabel(GraphSnapshot.Node node) {
        Object value = pickDisplayProperty(node);
        if (value == null)
            return node.labels().isEmpty() ? "?" : node.labels().get(0);
        String s = String.valueOf(value);
        return s.length() > MAX_LABEL_CHARS ? s.substring(0, MAX_LABEL_CHARS - 1) + "…" : s;
    }

    private static Object pickDisplayProperty(GraphSnapshot.Node node) {
        for (String preferred : List.of("name", "title", "id")) {
            Object v = node.properties().get(preferred);
            if (v != null) return v;
        }
        for (Map.Entry<String, Object> entry : node.properties().entrySet()) {
            if (entry.getValue() instanceof String) return entry.getValue();
        }
        return null;
    }

    private static Color textColorOn(Color fill) {
        double luminance = (0.299 * fill.getRed() + 0.587 * fill.getGreen() + 0.114 * fill.getBlue()) / 255.0;
        return luminance > 0.6 ? Color.BLACK : Color.WHITE;
    }
}
