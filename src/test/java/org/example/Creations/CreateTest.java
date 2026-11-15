package org.example.Creations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for Create. No server is needed: sending is replaced by a fake that records each call. */
class CreateTest {

    /** A Create that records the URL of each order instead of sending it. */
    static class FakeCreate extends Create {
        final List<String> sent_urls = new ArrayList<>();

        FakeCreate() throws Exception {
            super("1.2.3.4", "8003");
        }

        @Override
        protected ArrayList<String> select_closed_work_orders(int count) {
            ArrayList<String> closed = new ArrayList<>();
            for (int i = 1; i <= count; i++) closed.add("CLOSED-" + i);
            return closed;
        }

        @Override
        protected String send_maintenance(XML xml, String url) throws Exception {
            xml.update_complain_number(); // make sure the template can still be updated
            sent_urls.add(url);
            return "WO-" + sent_urls.size();
        }

        @Override
        protected String send_installation(XML xml, String url) throws Exception {
            xml.randomize_OM_Order_ID();
            sent_urls.add(url);
            return "WO-" + sent_urls.size();
        }
    }

    @Test
    void urls_use_the_given_ip_and_port() throws Exception {
        Create create = new Create("10.0.0.1", "9000");
        assertEquals("http://10.0.0.1:9000/HiveAPIs/resources/hivews/CreateOrder", create.get_URL_installation());
        assertEquals("http://10.0.0.1:9000/FCCHiveWS/WSPort", create.get_URL_DATA_Maintenance());
        assertEquals("http://10.0.0.1:9000/FCCWFMInteg-FCCWFMInteg-context-root/FCCWFMIntegPort", create.get_URL_FTTH_Maintenance());
    }

    @Test
    void distribute_orders_splits_evenly_with_remainder_first() throws Exception {
        Create create = new FakeCreate();
        assertArrayEquals(new int[]{2, 2, 1, 1, 1, 1, 1, 1}, create.distribute_orders(10, 8));
        assertArrayEquals(new int[]{1, 1, 1, 0, 0, 0, 0, 0}, create.distribute_orders(3, 8));
        assertArrayEquals(new int[]{2, 2, 2, 2}, create.distribute_orders(8, 4));
    }

    @Test
    void file_split_follows_the_given_ratio() {
        assertArrayEquals(new int[]{431, 431, 431, 431, 278, 278}, Create.split_by_weights(2280, Create.FILE_WEIGHTS));
        assertArrayEquals(new int[]{862, 862, 862, 862, 556, 556}, Create.split_by_weights(4560, Create.FILE_WEIGHTS));
        assertArrayEquals(new int[]{1, 1, 1, 0, 0, 0}, Create.split_by_weights(3, Create.FILE_WEIGHTS));
        assertArrayEquals(new int[]{43, 43, 43, 43, 28, 28}, Create.split_by_weights(228, Create.FILE_WEIGHTS));
        for (int n = 0; n <= 3000; n++) {
            assertEquals(n, Arrays.stream(Create.split_by_weights(n, Create.FILE_WEIGHTS)).sum());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 8, 10, 17, 100, 228})
    void sends_every_order_except_closed(int count) throws Exception {
        FakeCreate create = new FakeCreate();
        int[] sizes = Create.split_by_weights(count, Create.FILE_WEIGHTS);
        ArrayList<String> work_orders = create.genrate_work_orders(count);
        // Closed orders come from the database, so they are not sent
        assertEquals(count - sizes[Create.CLOSED], create.sent_urls.size());
        // Only the Opened and Re-assign orders are returned; the rest were already written
        assertEquals(sizes[Create.OPENED] + sizes[Create.RE_ASSIGN], work_orders.size());
    }

    @Test
    void orders_go_to_the_right_endpoints() throws Exception {
        FakeCreate create = new FakeCreate();
        create.genrate_work_orders(228); // files: 43, 43, 43, 43, 28, 28

        assertEquals(0, count(create.sent_urls, create.get_URL_FTTH_Maintenance()));
        assertEquals(43, count(create.sent_urls, create.get_URL_DATA_Maintenance())); // all of Zone Tasks
        assertEquals(228 - 43 - 43, count(create.sent_urls, create.get_URL_installation())); // minus Closed, not sent
    }

    @Test
    void new_sub_orders_are_sent_right_after_the_first_order() throws Exception {
        FakeCreate create = new FakeCreate();
        create.genrate_work_orders(228); // 43 My Tasks + 28 Assign = 71 FTTHNewSubVS, 43 DATA maintenance

        // First order is DATA maintenance, then the 71 FTTHNewSubVS (installation), then the next DATA order
        assertEquals(create.get_URL_DATA_Maintenance(), create.sent_urls.get(0));
        for (int i = 1; i <= 71; i++) {
            assertEquals(create.get_URL_installation(), create.sent_urls.get(i));
        }
        assertEquals(create.get_URL_DATA_Maintenance(), create.sent_urls.get(72));
    }

    @Test
    void returns_the_work_order_numbers_in_send_order() throws Exception {
        FakeCreate create = new FakeCreate();
        // 3 orders: 1 Closed (from the database, not sent), the 1 DATA maintenance order (Zone Tasks) is sent first,
        // then the 1 Opened installation order, which is returned
        assertEquals(Arrays.asList("WO-2"), create.genrate_work_orders(3));
    }

    private static long count(List<String> list, String value) {
        return list.stream().filter(value::equals).count();
    }
}
