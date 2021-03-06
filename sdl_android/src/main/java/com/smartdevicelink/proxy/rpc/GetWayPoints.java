package com.smartdevicelink.proxy.rpc;

import android.support.annotation.NonNull;

import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.rpc.enums.WayPointType;

import java.util.Hashtable;

public class GetWayPoints extends RPCRequest {
	public static final String KEY_WAY_POINT_TYPE = "wayPointType";

    public GetWayPoints() {
        super(FunctionID.GET_WAY_POINTS.toString());
    }

    public GetWayPoints(Hashtable<String, Object> hash) {
        super(hash);
    }

    public GetWayPoints(@NonNull WayPointType wayPointType) {
        this();
        setWayPointType(wayPointType);
    }

    public WayPointType getWayPointType() {
        return (WayPointType) getObject(WayPointType.class, KEY_WAY_POINT_TYPE);
    }

    public void setWayPointType(@NonNull WayPointType wayPointType) {
        setParameters(KEY_WAY_POINT_TYPE, wayPointType);
    }
}
