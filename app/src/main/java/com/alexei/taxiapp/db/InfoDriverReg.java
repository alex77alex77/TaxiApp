package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.alexei.taxiapp.driver.model.DataAuto;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.driver.model.ExOrderModel;
import com.alexei.taxiapp.driver.model.RedirectOrderModel;
import com.alexei.taxiapp.driver.model.SOSModel;
import com.alexei.taxiapp.server.model.MsgModel;

@Entity(tableName = "drivers_in_server")
public class InfoDriverReg {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    @ColumnInfo(name = "u_id")
    private String currUId;
    @ColumnInfo(name = "balance")
    private float balance;

    @ColumnInfo(name = "shift_status")
    private int shiftStatus;
    @ColumnInfo(name = "close_shift_time")
    private long closeShiftTime;
    @ColumnInfo(name = "finish_shift_time")
    private long finishTimeShift;
    @ColumnInfo(name = "open_shift_time")
    private long openShiftTime;
    @ColumnInfo(name = "call_sign")
    private long callSign;
    @ColumnInfo(name = "driver_u_id")
    private String driverUid = "";
    @ColumnInfo(name = "server_u_id")
    private String serverUid = "";
    @ColumnInfo(name = "status_to_host_srv")
    private int statusToHostSrv = -1;
    @ColumnInfo(name = "addition_data")
    private String additionData = "";
    @ColumnInfo(name = "type_transport")
    private String autoType = "";
    @Embedded
    private DataLocation dislocation = new DataLocation(0.0d, 0.0d);
    @ColumnInfo(name = "name")
    private String name = "";
    @ColumnInfo(name = "email")
    private String email = "";
    @ColumnInfo(name = "phone")
    private String phone = "";
    @ColumnInfo(name = "password")
    private String password;
    @ColumnInfo(name = "priority")
    private int priority = 0;
    @ColumnInfo(name = "created")
    private long timeCreate = 0;

    @Ignore
    private int statusShared = -1;
    @Ignore
    private RedirectOrderModel assignedOrder;
    @Ignore
    private DataAuto auto = new DataAuto("-", "-", "-");
    @Ignore
    private DataLocation location = new DataLocation();
    @Ignore
    private ExOrderModel exOrder;
    @Ignore
    private MsgModel message = new MsgModel();
    @Ignore
    private SOSModel sosModel;


    public InfoDriverReg() {
    }


    public InfoDriverReg(String currUId, String driverUid, int statusToHostSrv) {
        this.currUId = currUId;
        this.driverUid = driverUid;
        this.statusToHostSrv = statusToHostSrv;
    }

    public InfoDriverReg(String currUId,
                         float balance,
                         int shiftStatus,
                         long closeShiftTime,
                         long finishTimeShift,
                         long openShiftTime,
                         String additionData,
                         String name,
                         String password,
                         long callSign,
                         DataAuto auto,
                         String autoType,
                         String driverUid,
                         String email,
                         String phone,
                         String serverUid,
                         int statusToHostSrv,
                         int priority,
                         DataLocation dislocation,
                         long timeCreate) {
        this.currUId = currUId;
        this.balance = balance;
        this.shiftStatus = shiftStatus;
        this.closeShiftTime = closeShiftTime;
        this.finishTimeShift = finishTimeShift;
        this.openShiftTime = openShiftTime;
        this.additionData = additionData;
        this.name = name;
        this.password = password;
        this.callSign = callSign;
        this.auto = auto;
        this.autoType = autoType;
        this.driverUid = driverUid;
        this.email = email;
        this.phone = phone;
        this.serverUid = serverUid;
        this.statusToHostSrv = statusToHostSrv;
        this.priority = priority;
        this.dislocation = dislocation;
        this.timeCreate = timeCreate;
    }

    public MsgModel getMessage() {
        return this.message;
    }

    public void setMessage(MsgModel message2) {
        this.message = message2;
    }

    public DataLocation getLocation() {
        return this.location;
    }

    public void setLocation(DataLocation location2) {
        this.location = location2;
    }

    public RedirectOrderModel getAssignedOrder() {
        return this.assignedOrder;
    }

    public void setAssignedOrder(RedirectOrderModel assignedOrder2) {
        this.assignedOrder = assignedOrder2;
    }

    public ExOrderModel getExOrder() {
        return this.exOrder;
    }

    public void setExOrder(ExOrderModel exOrder2) {
        this.exOrder = exOrder2;
    }

    public SOSModel getSosModel() {
        return this.sosModel;
    }

    public void setSosModel(SOSModel sosModel2) {
        this.sosModel = sosModel2;
    }

    public int getStatusShared() {
        return this.statusShared;
    }

    public void setStatusShared(int statusShared2) {
        this.statusShared = statusShared2;
    }

    //--------------------------------------------------------------

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCurrUId() {
        return this.currUId;
    }

    public void setCurrUId(String currUId2) {
        this.currUId = currUId2;
    }

    public float getBalance() {
        return this.balance;
    }

    public void setBalance(float balance2) {
        this.balance = balance2;
    }

    public int getShiftStatus() {
        return this.shiftStatus;
    }

    public void setShiftStatus(int shiftStatus2) {
        this.shiftStatus = shiftStatus2;
    }

    public long getCloseShiftTime() {
        return this.closeShiftTime;
    }

    public void setCloseShiftTime(long closeShiftTime2) {
        this.closeShiftTime = closeShiftTime2;
    }

    public long getFinishTimeShift() {
        return this.finishTimeShift;
    }

    public void setFinishTimeShift(long finishTimeShift2) {
        this.finishTimeShift = finishTimeShift2;
    }

    public long getOpenShiftTime() {
        return this.openShiftTime;
    }

    public void setOpenShiftTime(long openShiftTime2) {
        this.openShiftTime = openShiftTime2;
    }

    public DataAuto getAuto() {
        return this.auto;
    }

    public void setAuto(DataAuto auto2) {
        this.auto = auto2;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name2) {
        this.name = name2;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password2) {
        this.password = password2;
    }

    public long getCallSign() {
        return this.callSign;
    }

    public void setCallSign(long callSign2) {
        this.callSign = callSign2;
    }

    public String getAutoType() {
        return this.autoType;
    }

    public void setAutoType(String autoType2) {
        this.autoType = autoType2;
    }

    public String getDriverUid() {
        return this.driverUid;
    }

    public void setDriverUid(String driverUid2) {
        this.driverUid = driverUid2;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email2) {
        this.email = email2;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone2) {
        this.phone = phone2;
    }

    public String getServerUid() {
        return this.serverUid;
    }

    public void setServerUid(String serverUid2) {
        this.serverUid = serverUid2;
    }

    public int getStatusToHostSrv() {
        return this.statusToHostSrv;
    }

    public void setStatusToHostSrv(int statusToHostSrv2) {
        this.statusToHostSrv = statusToHostSrv2;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority2) {
        this.priority = priority2;
    }

    public DataLocation getDislocation() {
        return this.dislocation;
    }

    public void setDislocation(DataLocation dislocation2) {
        this.dislocation = dislocation2;
    }

    public long getTimeCreate() {
        return this.timeCreate;
    }

    public void setTimeCreate(long timeCreate2) {
        this.timeCreate = timeCreate2;
    }

    public String getAdditionData() {
        return this.additionData;
    }

    public void setAdditionData(String additionData2) {
        this.additionData = additionData2;
    }

    public String toString() {
        return this.driverUid;
    }
}
