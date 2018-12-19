import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a command string sent from a client to the server, after it has been parsed into a
 * more convenient form. The {@code Command} abstract class has a concrete subclass corresponding to
 * each of the possible commands that can be issued by a client. The protocol specification contains
 * more information about the expected behavior of various commands.
 */
public abstract class Command {

    /**
     * The server-assigned ID of the user who sent the {@code Command}.
     */
    private int senderId;

    /**
     * The current nickname in use by the sender of the {@code command}.
     */
    private String sender;

    Command(int senderId, String sender) {
        this.senderId = senderId;
        this.sender = sender;
    }

    /**
     * Gets the user ID of the client who issued the {@code Command}.
     *
     * @return The user ID of the client who issued this command
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * Gets the nickname of the client who issued the {@code Command}.
     *
     * @return The nickname of the client who issued this command
     */
    public String getSender() {
        return sender;
    }

    /**
     * Processes the command and updates the server model accordingly.
     *
     * @param model An instance of the {@link ServerModelApi} class which represents the current
     *              state of the server.
     * @return A {@link Broadcast} object, informing clients about changes resulting from the
     *      command.
     */
    public abstract Broadcast updateServerModel(ServerModel model);

    /**
     * Returns {@code true} if two {@code Command}s are equal; that is, they produce the same string
     * representation.
     *
     * @param o the object to compare with {@code this} for equality
     * @return true iff both objects are non-null and equal to each other
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Command)) {
            return false;
        }
        return this.toString().equals(o.toString());
    }
}


//==============================================================================
// Command subclasses
//==============================================================================

/**
 * Represents a {@link Command} issued by a client to change his or her nickname.
 */
class NicknameCommand extends Command {
    private String newNickname;

    public NicknameCommand(int senderId, String sender, String newNickname) {
        super(senderId, sender);
        this.newNickname = newNickname;
    }

    
    @Override
    public Broadcast updateServerModel(ServerModel model) {
    	//creating a new collection with friends of user and user him/herself to broadcast to
    	String send = getSender();
    	int sendID = getSenderId();
    	Collection <String> usersToBroadcastTo = model.getOtherUsersInChannel(send);
    	usersToBroadcastTo.add(send); 
    	 if(model.getRegisteredUsers().contains(newNickname)) {
         	return Broadcast.error(this, ServerError.NAME_ALREADY_IN_USE);
         }
    	//changes nickname in both registered users map AND all the channels user was in
    	 //Notifies other users about nickname change 
    	Collection<Channel> channelsUserIsIn = model.getChannelWithUserNickname(send);
        if(ServerModel.isValidName(newNickname)) {
        	model.changeNickname(sendID, newNickname);
        	return Broadcast.okay(this, usersToBroadcastTo);
        }//error messages if name is already used and if invalid name 
      
      
       
        
        	return Broadcast.error(this, ServerError.INVALID_NAME); 
        
    }

    public String getNewNickname() {
        return newNickname;
    }

    @Override
    public String toString() {
        return String.format(":%s NICK %s", getSender(), newNickname);
    }
}

/**
 * Represents a {@link Command} issued by a client to create a new channel.
 */
class CreateCommand extends Command {
    private String channel;
    private boolean inviteOnly;

    public CreateCommand(int senderId, String sender, String channel, boolean inviteOnly) {
        super(senderId, sender);
        this.channel = channel;
        this.inviteOnly = inviteOnly;
    }
    
    @Override
    public Broadcast updateServerModel(ServerModel model) {
        //create channel if name is valid 
    	String sender = getSender();
    	Collection <String> newCollection = new TreeSet<String>();
    	newCollection.add(sender);
    	if(ServerModel.isValidName(channel)) {
        	model.createChannel(channel, getSender());
        	return Broadcast.okay(this, newCollection); 
        }
        //error message if the channel name already exists
        if(model.getChannels().contains(channel)) {
        	return Broadcast.error(this, ServerError.NAME_ALREADY_IN_USE); 
        }
        //otherwise return error message with invalid name -
        return Broadcast.error(this, ServerError.INVALID_NAME); 
        
    }

    public String getChannel() {
        return channel;
    }

    public boolean isInviteOnly() {
        return inviteOnly;
    }

    @Override
    public String toString() {
        int flag = inviteOnly ? 1 : 0;
        return String.format(":%s CREATE %s %d", getSender(), channel, flag);
    }
}

/**
 * Represents a {@link Command} issued by a client to join an existing channel.
 * All users in the channel (including the new one) should be notified about when 
 * a "join" occurs.
 */
class JoinCommand extends Command {
    private String channel;

    public JoinCommand(int senderId, String sender, String channel) {
        super(senderId, sender);
        this.channel = channel;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        if(model.getChannels().contains(channel)) {
        	//error message if channel is private 
            Channel c = model.getParticularChannel(channel);
            if(c.privateState()) {
            	return Broadcast.error(this, ServerError.JOIN_PRIVATE_CHANNEL);
            }
        	String send = getSender();
        	Set<String> userInChan = model.getParticularChannel(channel).getUsers();
        	String owner = model.getParticularChannel(channel).getOwner();
        	//add user who requested to join channel to the channel 
        	model.joinChannel(model.getParticularChannel(channel), send);
        	//ensures everyone else in channel gets notified 
        	return Broadcast.names(this, userInChan, 
        			owner);
            
        }
      
        
        //error message if channel does not exist 
        return Broadcast.error(this, ServerError.NO_SUCH_CHANNEL);
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return String.format(":%s JOIN %s", getSender(), channel);
    }
}

/**
 * Represents a {@link Command} issued by a client to send a message to all other clients in the
 * channel.
 */
class MessageCommand extends Command {
    private String channel;
    private String message;

    public MessageCommand(int senderId, String sender, String channel, String message) {
        super(senderId, sender);
        this.channel = channel;
        this.message = message;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        
        if(!model.getChannels().contains(channel)) {
    		return Broadcast.error(this, ServerError.NO_SUCH_CHANNEL); 
    	}
        Channel chan = model.getParticularChannel(channel);
        String send = getSender();
        Set<String> usersInChan = chan.getUsers();
        
        if(chan.getUsers().contains(send)) {
        	return Broadcast.okay(this, usersInChan); 
        }
    	
        //returns error message if the channel does not exist 
    	
    	return Broadcast.error(this, ServerError.USER_NOT_IN_CHANNEL); 
    }
    
    public String getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return String.format(":%s MESG %s :%s", getSender(), channel, message);
    }
}

/**
 * Represents a {@link Command} issued by a client to leave a channel.
 */
class LeaveCommand extends Command {
    private String channel;

    public LeaveCommand(int senderId, String sender, String channel) {
        super(senderId, sender);
        this.channel = channel;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
    	
    	//error message if no such channel exists 
    	if(!(model.getChannels().contains(channel))) {
        	return Broadcast.error(this, ServerError.NO_SUCH_CHANNEL);
        }
    	
    	String send = getSender();
        Channel chan = model.getParticularChannel(channel);
        Set<String> usersInChan = chan.getUsers();
        if(chan.getUsers().contains(send)) {
        	model.leaveChannel(channel, send);
        	//create a copy of the users in the channel so when user is removed, it does
        	//not affect the set of original users (which included the user himself) to broadcast to
        	Set<String> usersInChan2 = new TreeSet<String>();
        	usersInChan2.addAll(usersInChan);
        	usersInChan2.add(send);
        	System.out.println(send);
        	System.out.println(usersInChan2);
        	return Broadcast.okay(this, usersInChan2); 
        }
    	
    	return Broadcast.error(this, ServerError.USER_NOT_IN_CHANNEL); 
    	
        
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return String.format(":%s LEAVE %s", getSender(), channel);
    }
}

/**
 * Represents a {@link Command} issued by a client to add another client to an invite-only channel
 * owned by the sender.
 */
class InviteCommand extends Command {
    private String channel;
    private String userToInvite;

    public InviteCommand(int senderId, String sender, String channel, String userToInvite) {
        super(senderId, sender);
        this.channel = channel;
        this.userToInvite = userToInvite;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        //Error Messages
    	//if user to invite does not exist
    	if(model.getUserId(userToInvite) == -1){
        	return Broadcast.error(this, ServerError.NO_SUCH_USER);
        }
    	
    	//if no such channel
    	if((model.getChannels().contains(channel))) {
    		//if channel is public 
        	if(model.getParticularChannel(channel).privateState()) {
        		return Broadcast.error(this, ServerError.INVITE_TO_PUBLIC_CHANNEL);
        	}
    		//if sender is not the owner of specified channel
    		String send = getSender();
    		if(!(model.getParticularChannel(channel).getOwner().equals(send))) {
        		return Broadcast.error(this, ServerError.USER_NOT_OWNER);
        	}
    		
    		
    		
        	
    		Set<String> userInChan = model.getParticularChannel(channel).getUsers();
        	String owner = model.getParticularChannel(channel).getOwner();
        	//add user who requested to join channel to the channel 
        	model.joinChannel(model.getParticularChannel(channel), userToInvite);
        	//ensures everyone else in channel gets notified 
        	return Broadcast.names(this, userInChan, 
        			owner);
        }
    	
    	return Broadcast.error(this, ServerError.NO_SUCH_CHANNEL);
        	
        
        	
    	
    }

    public String getChannel() {
        return channel;
    }

    public String getUserToInvite() {
        return userToInvite;
    }

    @Override
    public String toString() {
        return String.format(":%s INVITE %s %s", getSender(), channel, userToInvite);
    }
}

/**
 * Represents a {@link Command} issued by a client to remove another client from a channel owned by
 * the sender. Everyone in the initial channel (including the user being kicked) should be informed
 * that the user was kicked.
 */
class KickCommand extends Command {
    private String channel;
    private String userToKick;

    public KickCommand(int senderId, String sender, String channel, String userToKick) {
        super(senderId, sender);
        this.channel = channel;
        this.userToKick = userToKick;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
    	//Error Messages
    	
    	//if user to invite does not exist
    	
    	if(!(model.getRegisteredUsers().contains(userToKick))){
        	return Broadcast.error(this, ServerError.NO_SUCH_USER);
        }
    
    	//if no such channel
    	if(model.getChannels().contains(channel)) {
    		//if sender is not the owner of specified channel
        	String send = getSender();
        	if(!model.getParticularChannel(channel).getOwner().equals(send)) {
        		return Broadcast.error(this, ServerError.USER_NOT_OWNER);
        	}
    		//if user to kick does not exist in channel
        	Channel c = model.getParticularChannel(channel);
        	Set<String> usersInChannel = new TreeSet<String>();
        	usersInChannel.addAll(model.getParticularChannel(channel).getUsers());
        	if(!c.getUsers().contains(userToKick)){
            	return Broadcast.error(this, ServerError.USER_NOT_IN_CHANNEL);
            }
        	
        	model.leaveChannel(channel, userToKick);
        	if(c.getOwner().equals(userToKick)) {
        		model.leaveChannel(channel, c.getOwner());
        	}
        	return Broadcast.okay(this, usersInChannel);
        }
    	return Broadcast.error(this, ServerError.NO_SUCH_CHANNEL);
    }

    @Override
    public String toString() {
        return String.format(":%s KICK %s %s", getSender(), channel, userToKick);
    }
}

