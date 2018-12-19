import java.util.*;


/**
 * The {@code ServerModel} is the class responsible for tracking the state of the server, including
 * its current users and the channels they are in.
 * This class is used by subclasses of {@link Command} to:
 *     1. handle commands from clients, and
 *     2. handle commands from {@link ServerBackend} to coordinate client connection/disconnection.
 */
public final class ServerModel implements ServerModelApi {
    

    /**
     * Constructs a {@code ServerModel} and initializes any collections needed for modeling the
     * server state.
     */
	
	private Map<Integer, String> registeredUsers;
	private Map<String, Channel> channels;
	private boolean inviteOnly;
	
    public ServerModel() {
        registeredUsers = new TreeMap<>(); 
        channels = new TreeMap<>(); 

    }


    //==========================================================================
    // Client connection handlers
    //==========================================================================

    /**
     * Informs the model that a client has connected to the server with the given user ID. The model
     * should update its state so that it can identify this user during later interactions. The
     * newly connected user will not yet have had the chance to set a nickname, and so the model
     * should provide a default nickname for the user.
     * Any user who is registered with the server (without being later deregistered) should appear
     * in the output of {@link #getRegisteredUsers()}.
     *
     * @param userId The unique ID created by the backend to represent this user
     * @return A {@link Broadcast} to the user with their new nickname
     */
    public Broadcast registerUser(int userId) {
        String nickname = generateUniqueNickname();
        registeredUsers.put(userId, nickname);
        return Broadcast.connected(nickname);
    }

    /**
     * Generates a unique nickname of the form "UserX", where X is the
     * smallest non-negative integer that yields a unique nickname for a user.
     * @return the generated nickname
     */
    private String generateUniqueNickname() {
        int suffix = 0;
        String nickname;
        Collection<String> existingUsers = getRegisteredUsers();
        do {
            nickname = "User" + suffix++;
        } while (existingUsers != null && existingUsers.contains(nickname));
        return nickname;
    }

    /**
     * Determines if a given nickname is valid or invalid (contains at least
     * one alphanumeric character, and no non-alphanumeric characters).
     * @param name The channel or nickname string to validate
     * @return true if the string is a valid name
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Informs the model that the client with the given user ID has disconnected from the server.
     * After a user ID is deregistered, the server backend is free to reassign this user ID to an
     * entirely different client; as such, the model should remove all state of the user associated
     * with the deregistered user ID. The behavior of this method if the given user ID is not
     * registered with the model is undefined.
     * Any user who is deregistered (without later being registered) should not appear in the output
     * of {@link #getRegisteredUsers()}.
     *
     * @param userId The unique ID of the user to deregister
     * @return A {@link Broadcast} instructing clients to remove the user from all channels
     */
    public Broadcast deregisterUser(int userId) {
        //get nickname of the user ID 
    	String nickname = registeredUsers.get(userId);
    	registeredUsers.remove(userId);
    	Set <String> otherUsersInChannel = (Set) getOtherUsersInChannel(nickname);
        
        //removes users from all the channels user is in 
        for(Map.Entry<String, Channel> mapEntry : channels.entrySet()) {
        	Channel chan = mapEntry.getValue(); 
        	if(mapEntry.getValue().getUsers().contains(nickname)) {
        		chan.removeUser(nickname);	
        	}
        	if(chan.getOwner().equals(nickname)) {
        		channels.remove(chan);
        	}
        	
        }
      //lets other users know that current user has disconnected 
        return Broadcast.disconnected(nickname, otherUsersInChannel);
    }


    //==========================================================================
    // Model update functions
    //==========================================================================

    //get all the other users in the channels that the user is in not including the user him or herself
    public Collection<String> getOtherUsersInChannel(String nickname){
    	Set<String> otherUsersInChannel = new TreeSet<>();
    	for(Map.Entry<String, Channel> mapEntry : channels.entrySet()) {
    		if(mapEntry.getValue().getUsers().contains(nickname)) {
    			otherUsersInChannel.addAll(mapEntry.getValue().getUsers());
    		}
    	}
    	otherUsersInChannel.remove(nickname);//needs to exclude the user him or herself 
    	return otherUsersInChannel;
    }
    
    //get the channels user is in with just user nickname as argument
    public Collection<Channel> getChannelWithUserNickname(String nickname){
    	Set<Channel> channelsWithUser = new TreeSet<>();
    	for(Map.Entry<String, Channel> mapEntry : channels.entrySet()) {
    		Collection<String> usersInChan = mapEntry.getValue().getUsers();
    		if(usersInChan.contains(nickname)) {
    			channelsWithUser.add(mapEntry.getValue());
    		}
    	}
    	 
    	return channelsWithUser;
    }

    //==========================================================================
    // Server model queries
    // These functions provide helpful ways to test the state of your model.
    // You may also use them in your implementation.
    //==========================================================================

    /**
     * Gets the user ID currently associated with the given nickname. The returned ID is -1 if the
     * nickname is not currently in use.
     *
     * @param nickname The nickname for which to get the associated user ID
     * @return The user ID of the user with the argued nickname if such a user exists, otherwise -1
     */
    public int getUserId(String nickname) {
        //finds target nickname in the map of registered users and returns key value 
    	
    	for(Map.Entry<Integer, String> mapEntry : registeredUsers.entrySet()) {
    		if(nickname.equals(mapEntry.getValue())) {
    			int userID = mapEntry.getKey();
    			return userID;
    		}
    	}
        return -1;
    }

    /**
     * Gets the nickname currently associated with the given user ID. The returned nickname is
     * null if the user ID is not currently in use.
     *
     * @param userId The user ID for which to get the associated nickname
     * @return The nickname of the user with the argued user ID if such a user exists, otherwise
     *          null
     */
    public String getNickname(int userId) {
    	//finds target userID in the map of registered users and returns key value 
    	for(Map.Entry<Integer, String> mapEntry : registeredUsers.entrySet()) {
    		if(userId == mapEntry.getKey()) {
    			String nickname = mapEntry.getValue();
    			return nickname;
    		}
    	}
        return null;
    }

    /**
     * Gets a collection of the nicknames of all users who are registered with the server. Changes
     * to the returned collection should not affect the server state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of registered user nicknames
     */
    public Collection<String> getRegisteredUsers() {
    	Collection<String> registeredUsersColl = new TreeSet<>(registeredUsers.values());
        return registeredUsersColl;
        
    }

    /**
     * Gets a collection of the names of all the channels that are present on the server. Changes to
     * the returned collection should not affect the server state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of channel names
     */
    public Collection<String> getChannels() {
        Collection<String> chans = new TreeSet<>(channels.keySet());
        return chans;
        }

    /**
     * Gets a collection of the nicknames of all the users in a given channel. The collection is
     * empty if no channel with the given name exists. Modifications to the returned collection
     * should not affect the server state.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get member nicknames
     * @return The collection of user nicknames in the argued channel
     */
    public Collection<String> getUsersInChannel(String channelName) {
    	Set<String> usersInChan = channels.get(channelName).getUsers(); 
    	Collection<String> users = new TreeSet<>(usersInChan);
    			return users;
        
    }

    /**
     * Gets the nickname of the owner of the given channel. The result is {@code null} if no
     * channel with the given name exists.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get the owner nickname
     * @return The nickname of the channel owner if such a channel exists, othewrise null
     */
    public String getOwner(String channelName) {
        return channels.get(channelName).getOwner();
        
    }
    
   public void changeNickname(int ID, String nickname) {
    	
    	String oldNickname = registeredUsers.get(ID);
    	registeredUsers.put(ID, nickname);
    	for(Channel chan : channels.values()) {
    		Set<String> usersInChan = chan.getUsers();
    		if(usersInChan.contains(oldNickname)) {
    			chan.removeUser(oldNickname); 
    			chan.addUser(nickname);	
    			String owner = chan.getOwner();
    			if(owner.equals(oldNickname)) {
    				chan.setOwnerName(nickname);
    			}
    		}
    	}
    }

    //Handle channel creation
    
    public void createChannel(String nameChannel, String owner) {
    	Channel chan = new Channel(owner);
    	//add new channel to the map of existing channels 
    	channels.put(nameChannel, chan);
    	chan.addUser(owner);
    	
    }
    
    //When users join a channel; adds users to a channel 
    
    public void joinChannel(Channel chan, String nickname) {
    	chan.addUser(nickname);
    }
    
    //When users leave a channel; deletes channel if owner is the one leaving 
    
    public void leaveChannel(String nameChannel, String nickname) {
    	if(nickname.equals(channels.get(nameChannel).getOwner())) {
    		channels.remove(nameChannel);
    	}else {
    		channels.get(nameChannel).removeUser(nickname);
    	}
    }
    
    //Return the channel in our map of channels given its name 
    public Channel getParticularChannel(String nameChannel) {
    	return channels.get(nameChannel);
    }
   
    
   
}
