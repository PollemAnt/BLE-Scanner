package com.example.blescanner.databinding;
import com.example.blescanner.R;
import com.example.blescanner.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class DeviceServiceBindingImpl extends DeviceServiceBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.nazwa, 1);
        sViewsWithIds.put(R.id.device, 2);
        sViewsWithIds.put(R.id.servisy, 3);
        sViewsWithIds.put(R.id.servises, 4);
        sViewsWithIds.put(R.id.charakterystyki, 5);
        sViewsWithIds.put(R.id.gatt_costam, 6);
        sViewsWithIds.put(R.id.led_button, 7);
        sViewsWithIds.put(R.id.favorite, 8);
    }
    // views
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public DeviceServiceBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 9, sIncludes, sViewsWithIds));
    }
    private DeviceServiceBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (androidx.cardview.widget.CardView) bindings[0]
            , (android.widget.TextView) bindings[5]
            , (android.widget.TextView) bindings[2]
            , (android.widget.ImageView) bindings[8]
            , (android.widget.TextView) bindings[6]
            , (android.widget.Button) bindings[7]
            , (android.widget.TextView) bindings[1]
            , (android.widget.TextView) bindings[4]
            , (android.widget.TextView) bindings[3]
            );
        this.cardViewServis.setTag(null);
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
        if (BR.gatt == variableId) {
            setGatt((android.bluetooth.BluetoothGattService) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setGatt(@Nullable android.bluetooth.BluetoothGattService Gatt) {
        this.mGatt = Gatt;
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
        // batch finished
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): gatt
        flag 1 (0x2L): null
    flag mapping end*/
    //end
}