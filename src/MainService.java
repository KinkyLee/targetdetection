import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;

//import com.sds.targetdetection.TargetDetection.TargetNode;
import TargetDetection.TargetNode;

public class MainService implements Runnable {

	static final int APP_ID = 0x11;
	private static final int SQL_STATEMENT = 1;
	private static final int EXTENDED_INS = 0x20;
	private static final int MAC_INS_TYPE = 0x02;
	private static final int PRELIMINARY_SHUTDOWN = 0x08;
	private static final int CLEAR_DATA = 0x07;
	StateOfApp stateOfApp;
	public static DatabaseHelper sDatabaseHelper;
	public static String sStorageLocation;
	SqlManager manager;

	TargetDetection mTargetDetection;

	public void report(String r) {
		System.out.println(r);
	}
	
	class QueryParam {
		String tableName;
		String projection;
		String selection;
		String[] selectionArgs;
		String sortOrder;
		float threshold;
		int region;
		int widthNarrowedDown;
		int heightNarrowedDown;
	}

	private QueryParam getQueryParam(String sql) {
		QueryParam queryParam = new QueryParam();
		String[] strs = sql.split(";");

		String[] uri = strs[0].split("/");
		queryParam.tableName = uri[uri.length-1];
		
		if (strs[1].equalsIgnoreCase("null")) {
			queryParam.projection = null;
		} else {
			queryParam.projection = strs[1];
		}
		
		if (strs[2].equalsIgnoreCase("null")) {
			queryParam.selection = null;
		} else {
			queryParam.selection = strs[2];
		}
		if (strs[3].equalsIgnoreCase("null")) {
			queryParam.selectionArgs = null;
		} else {
			queryParam.selectionArgs = strs[3].split(",");
		}
		if (strs[4].equalsIgnoreCase("null")) {
			queryParam.sortOrder = null;
		} else {
			queryParam.sortOrder = strs[4];
		}

		if (strs.length >= 6) {
			queryParam.threshold = Float.parseFloat(strs[5]);
		} else {
			queryParam.threshold = TargetDetection.DEFAULT_THRESHOLD;
		}

		if (strs.length >= 7) {
			queryParam.region = Integer.parseInt(strs[6]);
		} else {
			queryParam.region = TargetDetection.DEFAULT_REGION;
		}

		if (strs.length >= 9) {
			queryParam.widthNarrowedDown = Integer.parseInt(strs[7]);
			queryParam.heightNarrowedDown = Integer.parseInt(strs[8]);
		} else {
			queryParam.widthNarrowedDown = TargetDetection.DEFAULT_WIDTH_NARROWED_DOWN;
			queryParam.heightNarrowedDown = TargetDetection.DEFAULT_HEIGHT_NARROWED_DOWN;
		}
		return queryParam;
	}
	
	private void clearData(SqlManager manager) {
		DatabaseHelper.clearTable(manager);
		stateOfApp.setClearData();
	}

	public void onCreate() {
		sStorageLocation = System.getProperty("user.dir");

		manager = new SqlManager();
		if (manager != null) {
			manager.connectDB();
		} else {
			report("ContentObserver: Cannot Create SqlManager!");
		}

		stateOfApp = new StateOfApp();
		stateOfApp.setLaunchState();

	}

	public synchronized void run() {
		while (true) {
			String sqlString = "select zl_lx,zl_bh,zl_nr,zl_id from zl where zl_yyid = 17 and zl_zxjg=0";
			ResultSet rs = manager.executeQuery(sqlString);
			try {
				while (rs.next()) {
					report("A command is executing");
					long curInsID = rs.getLong(4);
					int curInsType = rs.getInt(1);
					int curInsNo = rs.getInt(2);
					String curInsContent = rs.getString(3);

					if (!((curInsType == MAC_INS_TYPE) && (curInsNo == PRELIMINARY_SHUTDOWN))) {
						try {
							sqlString = String.format("update zl set zl_zxjg=1 where zl_id=%d", curInsID);
							if (manager.executeUpdate(sqlString) <= 0) {
								report("ContentObserver: Update ins state failed!");
							}
						} catch (NullPointerException ignored) {
						}
					}

					switch (curInsType) {
					case MAC_INS_TYPE:
						if (curInsNo == CLEAR_DATA) {
							DatabaseHelper.clearTable(manager);
							clearData(manager);
						}
						break;
					case EXTENDED_INS: {
						switch (curInsNo) {
						case SQL_STATEMENT:
							QueryParam queryParam = getQueryParam(curInsContent);
								try {
									String sql = "select " + queryParam.projection + " from "  + queryParam.tableName + " where " + queryParam.selection;
									ResultSet rset = manager.executeQuery(sql);
									
									while (rset != null && rset.next()) {
										String fileName = rset.getString(3);
										String pathName = rset.getString(4);
										int width = rset.getInt(5);
										int height = rset.getInt(6);
										if ((fileName != null) && (pathName != null)) {
											mTargetDetection = new TargetDetection(width, height, 
													queryParam.threshold, queryParam.region,
													queryParam.widthNarrowedDown, queryParam.heightNarrowedDown);
											try {
												System.out.println(sStorageLocation + File.separator + pathName + File.separator + fileName);
												mTargetDetection.targetDetect(sStorageLocation + File.separator + pathName + File.separator + fileName);
												
											} catch (IOException e) {
												e.printStackTrace();
											}

											int picID = rset.getInt(1);
											sqlString = String.format(
													"insert into tx (txid, tx_rw_id, tx_mc, tx_lj)values(%d, %d, \'%s\', \'%s\')",
													picID, rset.getInt(2), fileName, pathName);
											long curID = manager.executeUpdate(sqlString);
											stateOfApp.setPicID(picID, (int) curID);

											int i = 0;
											
											while (i < mTargetDetection.mTargetNum) {
												if (mTargetDetection.mEigenList[i].iRegion >= mTargetDetection.mRegion) {
													String s = "insert into " + DatabaseHelper.MBD.TABLE_NAME
															+ "(" + DatabaseHelper.MBD.TXB_ID + ","
															+ DatabaseHelper.MBD.MBD_MJ + ","
															+ DatabaseHelper.MBD.MBD_LD + ","
															+ DatabaseHelper.MBD.YZB + ","
															+ DatabaseHelper.MBD.XZB
															+ ")VALUES("
															+ curID + ","
															+ mTargetDetection.mEigenList[i].iRegion + ","
															+ mTargetDetection.mEigenList[i].iLuminance + ","
															+ mTargetDetection.mEigenList[i].fPositionY + ","
															+ mTargetDetection.mEigenList[i].fPositionX
															+ ")";
													manager.executeUpdate(s);
													Thread.sleep(50);
													
													stateOfApp.setResult(i + 1, mTargetDetection.mEigenList[i].iRegion,
															mTargetDetection.mEigenList[i].iLuminance,
															mTargetDetection.mEigenList[i].fPositionY,
															mTargetDetection.mEigenList[i].fPositionX);
												}
												i++;
											}
										} else {
											String content = "Can not get fileName or pathName!";
											stateOfApp.setErrReport(content);
										}
									}
								

								} catch (NullPointerException e) {
									String content = "CameraService is not running!";
									stateOfApp.setErrReport(content);
									return;
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							break;
						default:
							break;
						}
					}
						break;
					default:
						break;
					}
					report("A command is executed");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String byteToString(byte[] bytes) {
		try {
			String sql = new String(bytes, "ASCII");
			return sql.replaceAll("\u0000", "");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static void main(String[] args) {
		
		MainService mainService = new MainService();
		mainService.onCreate();
		new Thread(mainService).start();
	}
}
