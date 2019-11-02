package com.teamez.ezrobotac;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import static com.teamez.ezrobotac.R.id;
import static com.teamez.ezrobotac.R.layout;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private List<DeviceInfo> mDeviceList;
    private MainActivity mContext;
    private RecyclerView mRecyclerView;

    private int lastSelectedPosition = -1;

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int itemPosition = mRecyclerView.getChildLayoutPosition(v);
            DeviceInfo item = mDeviceList.get(itemPosition);
            lastSelectedPosition = itemPosition;
            notifyDataSetChanged();
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

    List<DeviceInfo> getList() {
        return mDeviceList;
    }

    void setList(List<DeviceInfo> di) {
        mDeviceList = di;
    }

    void clear() {
        mDeviceList.clear();
        notifyDataSetChanged();
    }
    void resetAvailability() {
        for (DeviceInfo mdevice : mDeviceList) {
            mdevice.isAvailable = false;
        }
    }



    @Override
    public void onBindViewHolder (@NonNull DeviceViewHolder deviceViewHolder, int i) {
        DeviceInfo di = mDeviceList.get(i);
        if(di.mSSID == null || di.mPassword == null) {
            return;
        }
        deviceViewHolder.vDeviceName.setText(di.mSSID);
        deviceViewHolder.vDeviceIP.setText(di.mPassword);

        deviceViewHolder.selectionState.setChecked(lastSelectedPosition == i);

        if(di == null) {
            return;
        }
        //String name = di.mConnf.SSID.substring(1, di.mConnf.SSID.length() - 1);
        //deviceViewHolder.vDeviceName.setText(name);
        //deviceViewHolder.vDeviceIP.setText(di.mConnf.preSharedKey);

    }

    @Override
    public int getItemCount() {
        return mDeviceList.size();
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
         RadioButton selectionState;

        DeviceViewHolder(View v) {
            super(v);
            vDeviceName =  v.findViewById(id.device_name);
            vDeviceIP = v.findViewById(id.device_ip);
            selectionState = v.findViewById(id.robot_selected);
        }
    }
}
