package com.huanlezhang.aide;


import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {


    private ArrayList<BleDeviceInfoStore> mItemList;

    public SparseBooleanArray mCheckBoxArray = new SparseBooleanArray();

    public RecyclerViewAdapter(ArrayList<BleDeviceInfoStore> myDataset){
        mItemList = myDataset;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View view = layoutInflater.inflate(R.layout.ble_row_layout, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final BleDeviceInfoStore itemData = mItemList.get(i);
        viewHolder.mDeviceNameView.setText(itemData.name);
        viewHolder.mDeviceAddrView.setText(itemData.address);
        viewHolder.mDeviceRssiView.setText(Integer.toString(itemData.rssi));

        if (!mCheckBoxArray.get(i, false)) {
            viewHolder.mCheckBoxView.setChecked(false);
        } else {
            viewHolder.mCheckBoxView.setChecked(true);
        }
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    private void add(int position, BleDeviceInfoStore item) {
        mItemList.add(position, item);
        notifyItemInserted(position);
    }

    private void remove(int position) {
        mItemList.remove(position);
        notifyItemRemoved(position);
    }

    public int update(BleDeviceInfoStore item) {

        for (int i = 0; i < mItemList.size(); i++) {
            if (mItemList.get(i).address.equals(item.address)) {
                mItemList.set(i, item);
                notifyItemChanged(i);
                return i;
            }
        }

        // not found
        this.add(mItemList.size(), item);
        return mItemList.size() - 1;
    }

    public void clear() {
        int itemSize = mItemList.size();
        for (int i = 0; i < itemSize; i++) {
            this.remove(0);
        }
        mItemList.clear();
        mCheckBoxArray.clear();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mDeviceNameView;
        private TextView mDeviceAddrView;
        private TextView mDeviceRssiView;
        private CheckBox mCheckBoxView;

        private View mLayout;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            mLayout = itemView;

            mDeviceNameView = itemView.findViewById(R.id.ble_row_device_name);
            mDeviceAddrView = itemView.findViewById(R.id.ble_row_device_addr);
            mDeviceRssiView = itemView.findViewById(R.id.ble_row_device_rssi);

            mCheckBoxView = itemView.findViewById(R.id.ble_row_checkbox);
            mCheckBoxView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            CheckBox checkBox = (CheckBox) v;
            if (checkBox.isChecked()) {
                mCheckBoxArray.put(position, true);
            } else {
                mCheckBoxArray.put(position, false);
            }
        }
    }
}
