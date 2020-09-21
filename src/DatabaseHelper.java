import SqlManager;

class DatabaseHelper {
    interface TX {
        String TABLE_NAME = "tx";
        String ID = "id";
        String TXID= "txid";
        String TX_RW_ID = "tx_rw_id";
        String LJ = "tx_lj";
        String MC = "tx_mc";
    }

    interface MBD {
        String TABLE_NAME = "mbd";
        String ID = "id";
        String TXB_ID = "txb_id";
        String MBD_MJ = "mbd_mj";
        String MBD_LD = "mbd_ld";
        String YZB = "mbd_yzb";
        String XZB = "mbd_xzb";
    }

    public static void clearTable(SqlManager manager) {
        String[] sqls = new String[]{
                "delete from " + MBD.TABLE_NAME,
                "delete form " + TX.TABLE_NAME
        };

        for(String sql : sqls) {
        	manager.executeUpdate(sql);
        }
    }
}
