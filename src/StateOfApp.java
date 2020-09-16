package com.sds.targetdetection;

/**
 * 用于操作应用状态数据表及发送遥测指令
 */

class StateOfApp {
	
	SqlManager manager = null;
	int YYID = 0x11;
	
	public StateOfApp() {
		manager = new SqlManager();	
		if(manager != null) {
			manager.connectDB();
		}
		else {
			report("ContentObserver: Cannot Create SqlManager!");
		}
	}
	
	public void report(String r) {
		System.out.println(r);
	}
	
    /**
     * 发送实时遥测
     * @param content 实时遥测的内容
     */
	int sendRealTimeTelemetry(String content) {
		String sql = String.format("insert into ssyc set ssyc_yyid=%d, ssyc_nr='%s'", YYID, content);
		report("StateOfApp: " + content);
		return manager.executeUpdate(sql);
	}

    /**
     * 发送目标检测结果
     * @param num 目标点序号
     * @param region 目标点面积
     * @param luminance 目标点亮度
     * @param positionY 目标点Y坐标
     * @param positionX 目标点X坐标
     */
    void setResult(int num, int region, int luminance, double positionY, double positionX) {
        String content = "num:" + num + ", region:" + region
                + ", lum:" + luminance + ", Y:" + positionY + ", X:" + positionX;
        sendRealTimeTelemetry(content);
    }

    /**
     * 发送图像信息
     * @param picID 相机服务图像ID
     * @param curID 本服务图像表记录ID
     */
    void setPicID(int picID, int curID) {
        String content = "picID:" + picID + ", curID:" + curID;
        sendRealTimeTelemetry(content);
    }

    /**
     * 报告错误状态
     * @param content 错误内容
     */
    void setErrReport(String content) {
        sendRealTimeTelemetry(content);
    }

    /**
     * 发送遥测指令,通知本应用已启动
     */
    void setLaunchState() {
        sendRealTimeTelemetry("AppStarted:"+String.valueOf(YYID));
    }

    void setClearData() {
        sendRealTimeTelemetry("Clear Data Suc!");
    }
}
