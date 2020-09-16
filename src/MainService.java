package com.sds.targetdetection;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sds.targetdetection.TargetDetection.TargetNode;


/**
 * 主体程序
 */
public class MainService implements Runnable {

	/** 分配给本应用的应用ID号 */
	static final int APP_ID = 0x11;
	/** 自定义数据域为SQL语句的扩展指令的指令编号 */
	private static final int SQL_STATEMENT = 1;
	/** 指令类型为扩展指令 */
	private static final int EXTENDED_INS = 0x20;
	/** 指令类型为监控管理 */
	private static final int MAC_INS_TYPE = 0x02;
	/** 指令编号为预关机 */
	private static final int PRELIMINARY_SHUTDOWN = 0x08;
	/** 指令编号为应用数据清理 */
	private static final int CLEAR_DATA = 0x07;
	/** StateOfApp对象，用于操作应用状态数据表及发送遥测指令 */
	StateOfApp stateOfApp;
	/** DatabaseHelper对象 */
	public static DatabaseHelper sDatabaseHelper;
	/** 数据存储位置，除部分特殊的配置文件外，应用数据应保存在该目录下（或该目录的子目录下） */
	public static String sStorageLocation;
	/**用户操作数据库*/
	SqlManager manager;

	TargetDetection mTargetDetection;

	public void report(String r) {
		System.out.println(r);
	}
	
	/**
	 * 查询参数
	 */
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

	/**
	 * 获得查询参数
	 * 
	 * @param sql 原始字符串
	 * @return 查询参数
	 */
	private QueryParam getQueryParam(String sql) {
		QueryParam queryParam = new QueryParam();
		String[] strs = sql.split(";");

		//uri转为tableName
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
	
	/**
	 * 清理应用数据
	 */
	private void clearData(SqlManager manager) {
		// 清理数据表
		DatabaseHelper.clearTable(manager);
		stateOfApp.setClearData();
	}

	public void onCreate() {
		// 设置文件存储位置
		sStorageLocation = System.getProperty("user.dir");

		// 连接数据库
		manager = new SqlManager();
		if (manager != null) {
			manager.connectDB();
		} else {
			report("ContentObserver: Cannot Create SqlManager!");
		}

		// 初始化StateOfApp对象，并通知系统本应用已启动
		stateOfApp = new StateOfApp();
		stateOfApp.setLaunchState();

	}

	public synchronized void run() {
		while (true) {
			// 读取
			String sqlString = "select zl_lx,zl_bh,zl_nr,zl_id from zl where zl_yyid = 17 and zl_zxjg=0";
			ResultSet rs = manager.executeQuery(sqlString);
			try {
				while (rs.next()) {
					report("A command is executing");
					// 当前指令对应的ID
					long curInsID = rs.getLong(4);
					//指令类型
					int curInsType = rs.getInt(1);
					//指令编号
					int curInsNo = rs.getInt(2);
					//指令内容
					String curInsContent = rs.getString(3);			

					// 如果指令编号不为预关机，指令类型不为监控管理，则将指令置为已读
					if (!((curInsType == MAC_INS_TYPE) && (curInsNo == PRELIMINARY_SHUTDOWN))) {
						try {
							sqlString = String.format("update zl set zl_zxjg=1 where zl_id=%d", curInsID);
							if (manager.executeUpdate(sqlString) <= 0) {
								report("ContentObserver: Update ins state failed!");
							}
						} catch (NullPointerException ignored) {
						}
					}

					// 判断指令类型
					switch (curInsType) {
					// 监控管理服务
					case MAC_INS_TYPE:
						// 清除数据
						if (curInsNo == CLEAR_DATA) {
							DatabaseHelper.clearTable(manager);
							clearData(manager);
						}
						break;
					// 扩展指令
					case EXTENDED_INS: {
						// 判断指令编号
						switch (curInsNo) {
						// 指令编号为1
						case SQL_STATEMENT:
							// 获得查询参数
							QueryParam queryParam = getQueryParam(curInsContent);
								try {
									String sql = "select " + queryParam.projection + " from "  + queryParam.tableName + " where " + queryParam.selection;
									ResultSet rset = manager.executeQuery(sql);
									
									while (rset != null && rset.next()) {
										// 将查询结果拷贝至本地数据库，计算目标点信息，写入数据库
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

											// 将处理结果插入到数据库
											int picID = rset.getInt(1);// 对应相机服务中图片ID
											sqlString = String.format(
													"insert into tx (txid, tx_rw_id, tx_mc, tx_lj)values(%d, %d, \'%s\', \'%s\')",
													picID, rset.getInt(2), fileName, pathName);
											long curID = manager.executeUpdate(sqlString);
											// 遥测报告图像ID
											stateOfApp.setPicID(picID, (int) curID);

											// 检测结果输出
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
											// 报告文件名称或路径错误
											String content = "Can not get fileName or pathName!";
											stateOfApp.setErrReport(content);
										}
									}
								

								} catch (NullPointerException e) {
									// 需要下传的数据所在应用未启动
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
