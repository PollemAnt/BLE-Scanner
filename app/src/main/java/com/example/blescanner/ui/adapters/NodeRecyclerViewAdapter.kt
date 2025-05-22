package com.example.blescanner.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.example.blescanner.databinding.ItemNodeBinding
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node

class NodeRecyclerViewAdapter(private var listOfNodes: List<Node>) :
    RecyclerView.Adapter<NodeRecyclerViewAdapter.NodeRecyclerViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NodeRecyclerViewHolder {
        return NodeRecyclerViewHolder.from(parent)
    }

    override fun onBindViewHolder(
        holder: NodeRecyclerViewHolder,
        position: Int
    ) {
        val currentItem = listOfNodes[position]
        holder.bind(currentItem)
    }

    override fun getItemCount(): Int = listOfNodes.size

    fun setData(nodes: List<Node>) {
        listOfNodes = nodes
        notifyDataSetChanged()
    }

    class NodeRecyclerViewHolder(private val binding: ItemNodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(node: Node) {
            with(binding) {
                nodeName.text = node.name
                nodeAddress.text = node.toString()
                imageviewBin.setOnClickListener {
                    BluetoothMeshNetwork.removeNode(node)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): NodeRecyclerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemNodeBinding.inflate(layoutInflater, parent, false)
                return NodeRecyclerViewHolder(binding)
            }
        }
    }
}