package com.example.manytoonetest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.BaseMessageParams;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.GroupChannelListQuery;
import com.sendbird.android.GroupChannelParams;
import com.sendbird.android.Member;
import com.sendbird.android.MessageListParams;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.UserMessage;
import com.sendbird.android.UserMessageParams;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private Context mContext = this;
    private static final String TAG = "SendBird";
    public static final String APP_ID = "YOUR APPLICATION ID HERE";

    /**
     * This is your representative user ID
     * Must be a Moderator user
     */
    public static final String REPRESENTATIVE_USER_ID = "MODERATOR USER ID";
    public static final String REPRESENTATIVE_ACCESS_TOKEN = "MODERATOR ACCESS TOKEN";

    /**
     * Signed user ID
     * and role
     */
    private String mUserId;
    private int mRole;

    /**
     * Elements on screen
     */
    EditText mEditMessage;
    LinearLayout mLayoutGroupChannels;
    LinearLayout mLayoutChat;

    /**
     * Channel created for the conversation
     */
    GroupChannel mGroupChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        getExtrasFromMainActivity();
        mustShowChannelList();
        initElementsOnScreen();
        sendbirdLogin();
    }

    private void getExtrasFromMainActivity() {
        mRole = getIntent().getIntExtra("role", 1);
        mUserId = getIntent().getStringExtra("user_id");
    }

    private void mustShowChannelList() {
        if (mRole == 2) {
            ScrollView layoutForRepresentativesOnly = (ScrollView) findViewById(R.id.layoutForRepresentativesOnly);
            layoutForRepresentativesOnly.setVisibility(View.VISIBLE);
        }
    }

    private void initElementsOnScreen() {
        // Send chat message
        mEditMessage = (EditText)findViewById(R.id.editMessage);
        // Button for sending chat message
        Button butSendMessage = (Button)findViewById(R.id.butSendMessage);
        butSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        // Layout to show all group channels
        mLayoutGroupChannels = (LinearLayout) findViewById(R.id.layoutGroupChannels);
        // Layout for showing chat messages
        mLayoutChat = (LinearLayout)findViewById(R.id.layoutChat);
    }

    private void sendbirdLogin() {
        com.sendbird.android.SendBird.init(APP_ID, mContext);
        String userId = mRole == 1 ? mUserId : REPRESENTATIVE_USER_ID;
        String accessToken = mRole == 1 ? null : REPRESENTATIVE_ACCESS_TOKEN;
        com.sendbird.android.SendBird.connect(userId, accessToken, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                if (e != null) {    // Error.
                    Toast.makeText(mContext, "Error connecting as " + userId, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } else {
                    Toast.makeText(mContext, "User logged in with " + userId, Toast.LENGTH_LONG).show();
                    if (mRole == 1) {
                        joinUsersToChannel();
                    }
                    setChannelHandler();
                    refreshListOfGroupChannels();
                }
            }
        });
    }

    private void joinUsersToChannel() {
        List<String> users = new ArrayList<>();
        users.add(REPRESENTATIVE_USER_ID);
        String channelName = "Representative and " + mUserId;
        GroupChannelParams params = new GroupChannelParams()
                .setDistinct(true)
                .addUserIds(users)
                .setName(channelName);
        GroupChannel.createChannel(params, new GroupChannel.GroupChannelCreateHandler() {
            @Override
            public void onResult(GroupChannel groupChannel, SendBirdException e) {
                if (e != null) {
                    Toast.makeText(mContext, "Error creating channel!", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } else {
                    mGroupChannel = groupChannel;
                    addMessageToList("Channel: " + mGroupChannel.getName(), true);
                    listMessagesForGroupChannel(mGroupChannel);
                }
            }
        });
    }

    private void setChannelHandler() {
        SendBird.addChannelHandler("12345678888", new SendBird.ChannelHandler() {
            @Override
            public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                mGroupChannel = (GroupChannel) baseChannel;
                setTitle(mGroupChannel.getName());
                listMessagesForGroupChannel((GroupChannel)baseChannel);
            }
            @Override
            public void onChannelChanged(BaseChannel baseChannel) {
                refreshListOfGroupChannels();
                setTitle(baseChannel.getName());
                listMessagesForGroupChannel((GroupChannel)baseChannel);
            }
        });
    }

    private void refreshListOfGroupChannels() {
        GroupChannelListQuery listQuery = GroupChannel.createMyGroupChannelListQuery();
        listQuery.setIncludeEmpty(true);
        listQuery.setMemberStateFilter(GroupChannelListQuery.MemberStateFilter.JOINED);
        listQuery.setOrder(GroupChannelListQuery.Order.LATEST_LAST_MESSAGE);    // CHRONOLOGICAL, LATEST_LAST_MESSAGE, CHANNEL_NAME_ALPHABETICAL, and METADATA_VALUE_ALPHABETICAL
        listQuery.setLimit(100);
        listQuery.next(new GroupChannelListQuery.GroupChannelListQueryResultHandler() {
            @Override
            public void onResult(List<GroupChannel> list, SendBirdException e) {
                if (e != null) {
                    Toast.makeText(mContext, "Error recovering the list of channels", Toast.LENGTH_LONG).show();
                    return;
                }
                mLayoutGroupChannels.removeAllViews();
                List<GroupChannel> channels = list;
                for (int i=0; i < channels.size(); i++) {
                    final GroupChannel channel = channels.get(i);
                    Button button = new Button(mContext);
                    button.setText(channel.getName());
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mGroupChannel = channel;
                            addMessageToList("Channel: " + channel.getName(), true);
                            listMessagesForGroupChannel(channel);
                        }
                    });
                    mLayoutGroupChannels.addView(button);
                }
            }
        });
    }

    private void sendMessage() {
        String message = mEditMessage.getText().toString().trim();
        if (message.length() == 0) {
            return;
        }
        if (mGroupChannel == null) {
            Toast.makeText(mContext, "Select a channel first", Toast.LENGTH_LONG).show();
            return;
        }
        UserMessageParams params = new UserMessageParams()
                .setMessage(message)
                .setPushNotificationDeliveryOption(BaseMessageParams.PushNotificationDeliveryOption.DEFAULT); // Either DEFAULT or SUPPRESS
        mGroupChannel.sendUserMessage(params, new BaseChannel.SendUserMessageHandler() {
            @Override
            public void onSent(UserMessage userMessage, SendBirdException e) {
                if (e != null) {
                    Toast.makeText(mContext, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    return;
                } else {
                    setTitle(mGroupChannel.getName());
                    Toast.makeText(mContext, "Message sent", Toast.LENGTH_LONG).show();
                    addMessageToList(userMessage);
                    mEditMessage.setText("");
                }
            }
        });
    }

    private void addMessageToList(BaseMessage message) {
        boolean showLeft = true;
        try {
            if (message.getSender().getUserId() == mUserId && mRole == 1) {
                // Place at the right - I AM THE USER
                showLeft = false;
            }
            addMessageToList(message.getMessage(), showLeft);
        } catch(Exception e) {
            // This is a the representative
            if (mRole == 2) {
                showLeft = false;
            }
        }
    }

    private void addMessageToList(String message, boolean showLeft) {
        TextView tv = new TextView(mContext);
        tv.setText(message);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        // Define where the bubble will be
        params.gravity = showLeft ? Gravity.LEFT : Gravity.RIGHT;
        tv.setLayoutParams(params);
        // Add to the chat
        mLayoutChat.addView(tv);
    }

    private void listMessagesForGroupChannel(GroupChannel channel) {
        MessageListParams params = new MessageListParams();
        params.setNextResultSize(100);
        params.setReverse(false);
        Long timestamp = System.currentTimeMillis()/1000;
        channel.getMessagesByTimestamp(timestamp, params, new BaseChannel.GetMessagesHandler() {
            @Override
            public void onResult(List<BaseMessage> messages, SendBirdException e) {
                if (e != null) {
                    // Handle error.
                    return;
                }
                mLayoutChat.removeAllViews();
                for (int i=0; i < messages.size(); i++) {
                    BaseMessage message = messages.get(i);
                    addMessageToList(message);
                }
            }
        });
    }
}