package com.teamez.ezrobotcontrol;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import static com.teamez.ezrobotcontrol.R.id;
import static com.teamez.ezrobotcontrol.R.layout;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private List<DeviceInfo> mDeviceList;
    private MainActivity mContext;
    private RecyclerView mRecyclerView;

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int itemPosition = mRecyclerView.getChildLayoutPosition(v);
            DeviceInfo item = mDeviceList.get(itemPosition);
            mContext.connectDevice(v, item);
        }
    };

    DeviceListAdapter(MainActivity context, int layoutResource, List<DeviceInfo> devices, RecyclerView mRecyclerView) {
        super();
        this.mDeviceList = devices;
        this.mContext = context;
        this.mRecyclerView = mRecyclerView;
    }

    void addElement(DeviceInfo element){
        mDeviceList.add(element);
        notifyDataSetChanged();
    }

    void clear() {
        mDeviceList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }

    @Override
    public void onBindViewHolder (@NonNull DeviceViewHolder deviceViewHolder, int i) {
        DeviceInfo di = mDeviceList.get(i);
        if(di.deviceName == null || di.deviceAddress == null) {
            return;
        }
        deviceViewHolder.vDeviceName.setText(di.deviceName);
        deviceViewHolder.vDeviceIP.setText(di.deviceAddress);
        if(di.isConnected()) {
            deviceViewHolder.vConnected.setText("It's in");
        } else {
            //deviceViewHolder.vConnected.setText("NO");
        }
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(layout.device_list_item, viewGroup, false);
        itemView.setOnClickListener(mOnClickListener);
        return new DeviceViewHolder(itemView);
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {

         TextView vDeviceName;
         TextView vDeviceIP;
         TextView vConnected;

        DeviceViewHolder(View v) {
            super(v);
            vDeviceName =  v.findViewById(id.device_name);
            vDeviceIP =  v.findViewById(id.device_ip);
            vConnected = v.findViewById(id.connected_status);
        }

    }
}
