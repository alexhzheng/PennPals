import java.util.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class Channel implements Comparable {
	
	
	private Set<String> usersInChannelNames;
	private String owner;
	private String name;
	private boolean isPrivate;
	
	public Channel(String owner, Boolean isPrivate) {
		usersInChannelNames = new TreeSet<>();
		this.owner = owner;
		usersInChannelNames.add(owner);
		this.isPrivate = isPrivate;
	}

	public Channel(String owner) {
		this.owner = owner;
		this.isPrivate = false;
		usersInChannelNames = new TreeSet<>();
		usersInChannelNames.add(owner);
	}
	
	public Set <String> getUsers() {//gets users in particular channel 
		return usersInChannelNames;
	}
	
	public String getOwner() {
		return this.owner; 
	}
	
	public void setOwnerName(String newName) {
		this.owner = newName;
	}
	
	public boolean privateState() {
		return this.isPrivate;
	}
	

	public void removeUser(String user) {
		usersInChannelNames.remove(user);
	}

	public void addUser(String nickname) {
		usersInChannelNames.add(nickname);
		
	}
	
	public void isPrivateChange(Boolean changePrivateState) {
		this.isPrivate = isPrivate;
	}
	
	
	//Don't currently use this method, just kept here as filler so Channel could implement Comparable 
	public int compareTo(Object o) {
		return Integer.compare(1, 0);
		
	}
}
