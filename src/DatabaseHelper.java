package com.sds.targetdetection;

/**
 * 用于管理数据库的工具类
 */
class DatabaseHelper {
    /**图像数据表*/
    interface TX {
        /**表名*/
        String TABLE_NAME = "tx";
        /**文件ID*/
        String ID = "id";
        /**图像ID*/
        String TXID= "txid";
        /**图像任务ID*/
        String TX_RW_ID = "tx_rw_id";
        /**图像路径*/
        String LJ = "tx_lj";
        /**图像名称*/
        String MC = "tx_mc";
    }

    /**目标点数据表*/
    interface MBD {
        /**表名*/
        String TABLE_NAME = "mbd";
        /**ID*/
        String ID = "id";
        /**图像表ID*/
        String TXB_ID = "txb_id";
        /**目标点面积*/
        String MBD_MJ = "mbd_mj";
        /**目标点亮度*/
        String MBD_LD = "mbd_ld";
        /**Y坐标*/
        String YZB = "mbd_yzb";
        /**X坐标*/
        String XZB = "mbd_xzb";
    }

    /**
     * 清理数据表
     */
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
