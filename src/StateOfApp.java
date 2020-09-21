//package com.sds.targetdetection;

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
	
	int sendRealTimeTelemetry(String content) {
		String sql = String.format("insert into ssyc set ssyc_yyid=%d, ssyc_nr='%s'", YYID, content);
		report("StateOfApp: " + content);
		return manager.executeUpdate(sql);
	}

    void setResult(int num, int region, int luminance, double positionY, double positionX) {
        String content = "num:" + num + ", region:" + region
                + ", lum:" + luminance + ", Y:" + positionY + ", X:" + positionX;
        sendRealTimeTelemetry(content);
    }

    void setPicID(int picID, int curID) {
        String content = "picID:" + picID + ", curID:" + curID;
        sendRealTimeTelemetry(content);
    }

    void setErrReport(String content) {
        sendRealTimeTelemetry(content);
    }

    void setLaunchState() {
        sendRealTimeTelemetry("AppStarted:"+String.valueOf(YYID));
    }

    void setClearData() {
        sendRealTimeTelemetry("Clear Data Suc!");
    }
}
