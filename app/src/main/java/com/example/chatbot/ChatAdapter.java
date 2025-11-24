package com.example.chatbot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.BaseAdapter;

import java.util.ArrayList;

public class ChatAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<ChatMessage> messages;

    public ChatAdapter(Context context, ArrayList<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.chat_message_item, parent, false);
        }

        ChatMessage message = messages.get(position);
        TextView messageText = convertView.findViewById(R.id.messageText);
        ImageView botIcon = convertView.findViewById(R.id.botIcon);
        ImageView userIcon = convertView.findViewById(R.id.userIcon);

        messageText.setText(message.getMessage());

        if (message.isUser()) {
            userIcon.setVisibility(View.VISIBLE);
            botIcon.setVisibility(View.GONE);
            messageText.setBackgroundResource(R.drawable.user_bubble);
        } else {
            userIcon.setVisibility(View.GONE);
            botIcon.setVisibility(View.VISIBLE);
            messageText.setBackgroundResource(R.drawable.bot_bubble);
        }

        return convertView;
    }
}
