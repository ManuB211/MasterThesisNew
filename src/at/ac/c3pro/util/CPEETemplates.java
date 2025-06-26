package at.ac.c3pro.util;

public class CPEETemplates {

    public static String SKELETON = "<testset xmlns=\"http://cpee.org/ns/properties/2.0\">\n" +
            "  <executionhandler>ruby</executionhandler>\n" +
            "  <dataelements/>\n" +
            "  <endpoints>\n" +
            "    <timeout>https://cpee.org/services/timeout.php</timeout>\n" +
            "    <subprocess>https://cpee.org/flow/start/url/</subprocess>\n" +
            "    <send>https-post://cpee.org/ing/correlators/message/send/</send>\n" +
            "    <receive>https-get://cpee.org/ing/correlators/message/receive/</receive>\n" +
            "    <sync>https-post://cpee.org/ing/correlators/sync/</sync>\n" +
            "  </endpoints>\n" +
            "  <attributes>\n" +
            "    <creator>Manuel Bildhauer</creator>\n" +
            "    <info>?</info>\n" +
            "    <author>Manuel Bildhauer</author>\n" +
            "    <modeltype>CPEE</modeltype>\n" +
            "    <guarded>none</guarded>\n" +
            "    <guarded_id/>\n" +
            "    <model_uuid>ea4decc8-a23b-41f8-a054-e4bd78153625</model_uuid>\n" +
            "    <model_version/>\n" +
            "    <theme>extended</theme>\n" +
            "    <design_stage>development</design_stage>\n" +
            "    <design_dir>Theses.dir/Manuel Bildhauer.dir</design_dir>\n" +
            "  </attributes>\n" +
            "  <description>" +
            "    <description xmlns=\"http://cpee.org/ns/description/1.0\">\n" +
            " ? " +
            "    \n</description>" +
            "  </description>" +
            "  <transformation>\n" +
            "    <description type=\"copy\"/>\n" +
            "    <dataelements type=\"none\"/>\n" +
            "    <endpoints type=\"none\"/>\n" +
            "  </transformation>\n" +
            "</testset>";


    public static String PARALLEL_START = "<parallel wait=\"-1\" cancel=\"last\">\n";
    public static String PARALLEL_END = "</parallel>\n";

    public static String PARALLEL_BRANCH_START = "<parallel_branch>\n";
    public static String PARALLEL_BRANCH_END = "</parallel_branch>\n";

    public static String EXCLUSIVE_START = "<choose mode=\"exclusive\">\n";
    public static String EXCLUSIVE_END = "</choose>\n";

    public static String EXCLUSIVE_BRANCH_START = "<alternative>\n";
    public static String EXCLUSIVE_BRANCH_END = "</alternative>\n";


    private static String TIMEOUT_TEMPLATE = "<call id=\"?\" endpoint=\"timeout\">\n" +
            "            <parameters>\n" +
            "              <label/>\n" +
            "              <method>:post</method>\n" +
            "              <arguments>\n" +
            "                <timeout>?</timeout>\n" +
            "              </arguments>\n" +
            "            </parameters>\n" +
            "            <annotations>\n" +
            "              <_generic/>\n" +
            "              <_timing>\n" +
            "                <_timing_weight/>\n" +
            "                <_timing_avg/>\n" +
            "                <explanations/>\n" +
            "              </_timing>\n" +
            "              <_shifting>\n" +
            "                <_shifting_type>Duration</_shifting_type>\n" +
            "              </_shifting>\n" +
            "              <_context_data_analysis>\n" +
            "                <probes/>\n" +
            "                <ips/>\n" +
            "              </_context_data_analysis>\n" +
            "              <report>\n" +
            "                <url/>\n" +
            "              </report>\n" +
            "              <_notes>\n" +
            "                <_notes_general/>\n" +
            "              </_notes>\n" +
            "            </annotations>\n" +
            "            <documentation>\n" +
            "              <input/>\n" +
            "              <output/>\n" +
            "              <implementation>\n" +
            "                <description/>\n" +
            "              </implementation>\n" +
            "            </documentation>\n" +
            "          </call>\n";

    public static String getTimeout(String callId, Integer duration) {
        String rst = TIMEOUT_TEMPLATE.replaceFirst("\\?", callId);
        rst = rst.replaceFirst("\\?", duration + "");

        return rst;
    }

    private static String TIMEOUT_WITH_LABEL_TEMPLATE = "<call id=\"?\" endpoint=\"timeout\">\n" +
            "            <parameters>\n" +
            "              <label>Resoure Share Simulation</label>\n" +
            "              <method>:post</method>\n" +
            "              <arguments>\n" +
            "                <timeout>5</timeout>\n" +
            "              </arguments>\n" +
            "            </parameters>\n" +
            "            <annotations>\n" +
            "              <_generic/>\n" +
            "              <_timing>\n" +
            "                <_timing_weight/>\n" +
            "                <_timing_avg/>\n" +
            "                <explanations/>\n" +
            "              </_timing>\n" +
            "              <_shifting>\n" +
            "                <_shifting_type>Duration</_shifting_type>\n" +
            "              </_shifting>\n" +
            "              <_context_data_analysis>\n" +
            "                <probes/>\n" +
            "                <ips/>\n" +
            "              </_context_data_analysis>\n" +
            "              <report>\n" +
            "                <url/>\n" +
            "              </report>\n" +
            "              <_notes>\n" +
            "                <_notes_general/>\n" +
            "              </_notes>\n" +
            "            </annotations>\n" +
            "            <documentation>\n" +
            "              <input/>\n" +
            "              <output/>\n" +
            "              <implementation>\n" +
            "                <description/>\n" +
            "              </implementation>\n" +
            "            </documentation>\n" +
            "          </call>\n";

    public static String getTimeoutWithLabel(String callId) {
        return TIMEOUT_WITH_LABEL_TEMPLATE.replaceFirst("\\?", callId);
    }


    private static String MESSAGE_SEND_TEMPLATE = "<call id=\"?\" endpoint=\"send\">\n" +
            "            <parameters>\n" +
            "              <label/>\n" +
            "              <arguments>\n" +
            "                <id>?</id>\n" +
            "                <message>Hello World</message>\n" +
            "                <ttl>0</ttl>\n" +
            "              </arguments>\n" +
            "            </parameters>\n" +
            "            <annotations>\n" +
            "              <_generic/>\n" +
            "              <_timing>\n" +
            "                <_timing_weight/>\n" +
            "                <_timing_avg/>\n" +
            "                <explanations/>\n" +
            "              </_timing>\n" +
            "              <_shifting>\n" +
            "                <_shifting_type>Duration</_shifting_type>\n" +
            "              </_shifting>\n" +
            "              <_context_data_analysis>\n" +
            "                <probes/>\n" +
            "                <ips/>\n" +
            "              </_context_data_analysis>\n" +
            "              <report>\n" +
            "                <url/>\n" +
            "              </report>\n" +
            "              <_notes>\n" +
            "                <_notes_general/>\n" +
            "              </_notes>\n" +
            "            </annotations>\n" +
            "            <documentation>\n" +
            "              <input/>\n" +
            "              <output/>\n" +
            "              <implementation>\n" +
            "                <description/>\n" +
            "              </implementation>\n" +
            "            </documentation>\n" +
            "          </call>\n";

    /**
     * For now every message is just "Hello World"
     */
    public static String getMessageSend(String callId, String id) {
        String rst = MESSAGE_SEND_TEMPLATE.replaceFirst("\\?", callId);
        rst = rst.replaceFirst("\\?", id);
        return rst;
    }

    private static String MESSAGE_RECEIVE_TEMPLATE = "<call id=\"?\" endpoint=\"receive\">\n" +
            "                <parameters>\n" +
            "                  <label/>\n" +
            "                  <arguments>\n" +
            "                    <id>?</id>\n" +
            "                    <ttl>0</ttl>\n" +
            "                    <delete>true</delete>\n" +
            "                  </arguments>\n" +
            "                </parameters>\n" +
            "                <annotations>\n" +
            "                  <_generic/>\n" +
            "                  <_timing>\n" +
            "                    <_timing_weight/>\n" +
            "                    <_timing_avg/>\n" +
            "                    <explanations/>\n" +
            "                  </_timing>\n" +
            "                  <_shifting>\n" +
            "                    <_shifting_type>Duration</_shifting_type>\n" +
            "                  </_shifting>\n" +
            "                  <_context_data_analysis>\n" +
            "                    <probes/>\n" +
            "                    <ips/>\n" +
            "                  </_context_data_analysis>\n" +
            "                  <report>\n" +
            "                    <url/>\n" +
            "                  </report>\n" +
            "                  <_notes>\n" +
            "                    <_notes_general/>\n" +
            "                  </_notes>\n" +
            "                </annotations>\n" +
            "                <documentation>\n" +
            "                  <input/>\n" +
            "                  <output/>\n" +
            "                  <implementation>\n" +
            "                    <description/>\n" +
            "                  </implementation>\n" +
            "                </documentation>\n" +
            "              </call>\n";

    public static String getMessageReceive(String callId, String id) {
        String rst = MESSAGE_RECEIVE_TEMPLATE.replaceFirst("\\?", callId);
        rst = rst.replaceFirst("\\?", id);
        return rst;
    }

    private static String SYNC_TEMPLATE = "<call id=\"?\" endpoint=\"sync\">\n" +
            "                <parameters>\n" +
            "                  <label/>\n" +
            "                  <arguments>\n" +
            "                    <id>?</id>\n" +
            "                    <amount>2</amount>\n" +
            "                  </arguments>\n" +
            "                </parameters>\n" +
            "                <annotations>\n" +
            "                  <_generic/>\n" +
            "                  <_timing>\n" +
            "                    <_timing_weight/>\n" +
            "                    <_timing_avg/>\n" +
            "                    <explanations/>\n" +
            "                  </_timing>\n" +
            "                  <_shifting>\n" +
            "                    <_shifting_type>Duration</_shifting_type>\n" +
            "                  </_shifting>\n" +
            "                  <_context_data_analysis>\n" +
            "                    <probes/>\n" +
            "                    <ips/>\n" +
            "                  </_context_data_analysis>\n" +
            "                  <report>\n" +
            "                    <url/>\n" +
            "                  </report>\n" +
            "                  <_notes>\n" +
            "                    <_notes_general/>\n" +
            "                  </_notes>\n" +
            "                </annotations>\n" +
            "                <documentation>\n" +
            "                  <input/>\n" +
            "                  <output/>\n" +
            "                  <implementation>\n" +
            "                    <description/>\n" +
            "                  </implementation>\n" +
            "                </documentation>\n" +
            "              </call>\n";

    public static String getSync(String callId, String id) {
        String rst = SYNC_TEMPLATE.replaceFirst("\\?", callId);
        rst = rst.replaceFirst("\\?", id);
        return rst;
    }
}
