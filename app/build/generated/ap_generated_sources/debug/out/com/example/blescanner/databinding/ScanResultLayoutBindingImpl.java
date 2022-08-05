package com.example.blescanner.databinding;
import com.example.blescanner.R;
import com.example.blescanner.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ScanResultLayoutBindingImpl extends ScanResultLayoutBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.signal_logo, 5);
        sViewsWithIds.put(R.id.blinky_logo, 6);
        sViewsWithIds.put(R.id.connect_button, 7);
    }
    // views
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ScanResultLayoutBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 8, sIncludes, sViewsWithIds));
    }
    private ScanResultLayoutBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.ImageView) bindings[6]
            , (androidx.cardview.widget.CardView) bindings[0]
            , (android.widget.Button) bindings[7]
            , (android.widget.TextView) bindings[2]
            , (android.widget.TextView) bindings[4]
            , (android.widget.TextView) bindings[1]
            , (android.widget.ImageView) bindings[5]
            , (android.widget.TextView) bindings[3]
            );
        this.cardView.setTag(null);
        this.deviceAddress.setTag(null);
        this.deviceDistance.setTag(null);
        this.deviceName.setTag(null);
        this.signalStrength.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x2L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.result == variableId) {
            setResult((android.bluetooth.le.ScanResult) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setResult(@Nullable android.bluetooth.le.ScanResult Result) {
        this.mResult = Result;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.result);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        android.bluetooth.BluetoothDevice resultDevice = null;
        java.lang.String javaLangStringSignalResultRssi = null;
        java.lang.String javaLangStringAddressResultDeviceAddress = null;
        java.lang.String resultDeviceAddress = null;
        java.lang.String resultDeviceNameJavaLangObjectNullResultDeviceNameJavaLangStringUnknown = null;
        android.bluetooth.le.ScanResult result = mResult;
        boolean resultDeviceNameJavaLangObjectNull = false;
        java.lang.String resultDeviceName = null;
        int resultRssi = 0;

        if ((dirtyFlags & 0x3L) != 0) {



                if (result != null) {
                    // read result.device
                    resultDevice = result.getDevice();
                    // read result.rssi
                    resultRssi = result.getRssi();
                }


                if (resultDevice != null) {
                    // read result.device.address
                    resultDeviceAddress = resultDevice.getAddress();
                    // read result.device.name
                    resultDeviceName = resultDevice.getName();
                }
                // read ("Signal: ") + (result.rssi)
                javaLangStringSignalResultRssi = ("Signal: ") + (resultRssi);


                // read ("Address: ") + (result.device.address)
                javaLangStringAddressResultDeviceAddress = ("Address: ") + (resultDeviceAddress);
                // read result.device.name != null
                resultDeviceNameJavaLangObjectNull = (resultDeviceName) != (null);
            if((dirtyFlags & 0x3L) != 0) {
                if(resultDeviceNameJavaLangObjectNull) {
                        dirtyFlags |= 0x8L;
                }
                else {
                        dirtyFlags |= 0x4L;
                }
            }
        }
        // batch finished

        if ((dirtyFlags & 0x3L) != 0) {

                // read result.device.name != null ? result.device.name : "Unknown"
                resultDeviceNameJavaLangObjectNullResultDeviceNameJavaLangStringUnknown = ((resultDeviceNameJavaLangObjectNull) ? (resultDeviceName) : ("Unknown"));
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.deviceAddress, javaLangStringAddressResultDeviceAddress);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.deviceName, resultDeviceNameJavaLangObjectNullResultDeviceNameJavaLangStringUnknown);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.signalStrength, javaLangStringSignalResultRssi);
        }
        if ((dirtyFlags & 0x2L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.deviceDistance, "COS TAM: ");
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): result
        flag 1 (0x2L): null
        flag 2 (0x3L): result.device.name != null ? result.device.name : "Unknown"
        flag 3 (0x4L): result.device.name != null ? result.device.name : "Unknown"
    flag mapping end*/
    //end
}