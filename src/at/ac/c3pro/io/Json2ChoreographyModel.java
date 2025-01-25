package at.ac.c3pro.io;

public class Json2ChoreographyModel {
    private String modelName;
    private final String json = "";

    public static String XOR = "XOR";
    public static String AND = "AND";
    public static String OR = "OR";

    public static int gwcounter = 0;

    public Json2ChoreographyModel() {
        // TODO Auto-generated constructor stub
    }
    /*
     * public static IChoreographyModel convert(String json) throws
     * SerializationException { try { return convert(new JSONObject(json)); } catch
     * (JSONException e) { throw new SerializationException(e.getMessage()); } }
     */
    /*
     * public static IChoreographyModel convert(JSONObject json) throws
     * SerializationException { IChoreographyModel choreoModel = null;
     * MultiDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode> choreoGraph
     * =null; try { //choreoModel = new ChoreographyModel(json.getString("name"));
     * choreoGraph = new
     * MultiDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode>();
     * //Map<String, IChoreographyNode> nodes = new HashMap<String,
     * IChoreographyNode>(); JSONArray tasks = json.getJSONArray("tasks"); for (int
     * i = 0; i < tasks.length(); i++) { String id =
     * tasks.getJSONObject(i).getString("id"); String name =
     * tasks.getJSONObject(i).getString("label"); Interaction I = new Interaction();
     * choreoGraph.addVertex(I); } JSONArray gateways =
     * json.getJSONArray("gateways"); for (int i = 0; i < gateways.length(); i++) {
     * String id = gateways.getJSONObject(i).getString("id"); String name = null;
     * //gateways.getJSONObject(i).getString("label"); if (name == null ||
     * name.isEmpty()) name = "gw" + gwcounter++; Gateway = Integer vertex =
     * graph.addVertex(name); map.put(id, vertex); rmap.put(vertex, id);
     * gwmap.put(vertex, determineGatewayType(gateways.getJSONObject(i))); }
     * JSONArray flows = json.getJSONArray("flows"); for (int i = 0; i <
     * flows.length(); i++) { int from, to; if
     * (map.containsKey(flows.getJSONObject(i).getString("src"))) from =
     * map.get(flows.getJSONObject(i).getString("src")); else throw new
     * Exception("Unknown node " + flows.getJSONObject(i).getString("src") +
     * " was referenced by a flow as 'src'."); if
     * (map.containsKey(flows.getJSONObject(i).getString("tgt"))) to =
     * map.get(flows.getJSONObject(i).getString("tgt")); else throw new
     * Exception("Unknown node " + flows.getJSONObject(i).getString("tgt") +
     * " was referenced by a flow as 'tgt'.");
     *
     * graph.addEdge(from, to); } } catch (JSONException e) { throw new
     * Exception(e.getMessage()); } return graph; }
     *
     * private static GWType determineGatewayType(JSONObject obj) throws Exception {
     * if (obj.has("type")) { String type = ""; try { type = obj.getString("type");
     * } catch(JSONException e) { throw new Exception(e.getMessage()); } type =
     * type.toUpperCase(); if (type.equals(XOR)) return GWType.XOR; if
     * (type.equals(AND)) return GWType.AND; if (type.equals(OR)) return GWType.OR;
     * } throw new Exception("Couldn't determine GatewayType."); }
     *
     *
     * public BPMN2Helper_oldJSON(String model_path_tpl, String model_name) throws
     * Exception { super(); this.modelName = model_name; String fileName =
     * String.format(model_path_tpl, model_name); if (logger.isInfoEnabled())
     * logger.info("Reading BPMN 2.0 file: " + fileName);
     *
     * BufferedReader in = new BufferedReader(new FileReader(fileName));
     *
     * String line = null; while ((line = in.readLine()) != null) json += line;
     *
     * initGraph(); if (logger.isInfoEnabled()) logger.info("Graph created"); }
     *
     * protected void initGraph() { if (logger.isInfoEnabled())
     * logger.info("Creating graph");
     *
     * try { graph = convert(new JSONObject(json)); } catch (JSONException e) {
     * e.printStackTrace(); } catch (Exception e) { e.printStackTrace(); } }
     *
     * public String getModelName() { return this.modelName; }
     *
     */
}
