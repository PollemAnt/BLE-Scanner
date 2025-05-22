package com.example.blescanner.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.example.blescanner.databinding.ItemSubnetBinding
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet

class SubnetRecyclerViewAdapter(
    private var listOfSubnet: List<Subnet>,
    val onSubnetClicked: (Subnet) -> Unit
) :
    RecyclerView.Adapter<SubnetRecyclerViewAdapter.SubnetRecyclerViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubnetRecyclerViewHolder {
        return SubnetRecyclerViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: SubnetRecyclerViewHolder, position: Int) {
        val currentItem = listOfSubnet[position]
        holder.bind(currentItem)
        holder.itemView.setOnClickListener {
            onSubnetClicked(currentItem)
        }
    }

    override fun getItemCount(): Int = listOfSubnet.size

    fun setData(subnets: List<Subnet>) {
        listOfSubnet = subnets
        notifyDataSetChanged()
    }

    class SubnetRecyclerViewHolder(private val binding: ItemSubnetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(subnet: Subnet) {
            with(binding) {
                subnetName.text = subnet.name
                subnetNetkey.text = subnet.toString().drop(11).dropLast(1)
                subnetNodesSize.text = "Number of Nodes: ${subnet.nodes.size}"

                imageviewBin.setOnClickListener {
                    BluetoothMeshNetwork.removeSubnet(subnet)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): SubnetRecyclerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemSubnetBinding.inflate(layoutInflater, parent, false)
                return SubnetRecyclerViewHolder(binding)
            }
        }
    }
}