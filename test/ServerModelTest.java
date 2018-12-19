import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;

public class ServerModelTest {
    private ServerModel model;

    @Before
    public void setUp() {
        // We initialize a fresh ServerModel for each test
        model = new ServerModel();
    }

    /* Here is an example test that checks the functionality of your changeNickname error handling.
     * Each line has commentary directly above it which you can use as a framework for the remainder
     * of your tests.
     */
    @Test
    public void testInvalidNickname() {
        // A user must be registered before their nickname can be changed, so we first register a
        // user with an arbitrarily chosen id of 0.
        model.registerUser(0);

        // We manually create a Command that appropriately tests the case we are checking.
        // In this case, we create a NicknameCommand whose new Nickname is invalid.
        Command command = new NicknameCommand(0, "User0", "!nv@l!d!");

        // We manually create the expected Broadcast using the Broadcast factory methods.
        // In this case, we create an error Broadcast with our command and an INVALID_NAME error.
        Broadcast expected = Broadcast.error(command, ServerError.INVALID_NAME);

        // We then get the actual Broadcast returned by the method we are trying to test.
        // In this case, we use the updateServerModel method of the NicknameCommand.
        Broadcast actual = command.updateServerModel(model);

        // The first assertEquals call tests whether the method returns the appropriate broacast.
        assertEquals("Broadcast", expected, actual);

        // We also want to test whether the state has been correctly changed.
        // In this case, the state that would be affected is the user's Collection.
        Collection<String> users = model.getRegisteredUsers();

        // We now check to see if our command updated the state appropriately.
        // In this case, we first ensure that no additional users have been added.
        assertEquals("Number of registered users", 1, users.size());

        // We then check if the username was updated to an invalid value(it should not have been).
        assertTrue("Old nickname still registered", users.contains("User0"));

        // Finally, we check that the id 0 is still associated with the old, unchanged nickname.
        assertEquals("User with id 0 nickname unchanged", "User0", model.getNickname(0));
    }

    /*
     * Your TAs will be manually grading the tests you write in this file.
     * Don't forget to test both the public methods you have added to your
     * ServerModel class as well as the behavior of the server in different
     * scenarios.
     * You might find it helpful to take a look at the tests we have already
     * provided you with in ChannelsMessagesTest, ConnectionNicknamesTest,
     * and InviteOnlyTest.
     */
    @Test
    public void testGetUserId() {
    	model.registerUser(7);
    	Command command = new NicknameCommand(7, "User 7", "valid7");
    	command.updateServerModel(model);
    	assertEquals("get ID of user", 7, model.getUserId("valid7"));//should still return correct ID even when nickname is changed 
    	
    }
    
    @Test
    public void testDeregisterUserCases() {
    	model.registerUser(8);
    	model.deregisterUser(8);
    	model.registerUser(9);
    	Command command = new NicknameCommand(9, "User 9", "valid9");
    	Command command2 = new NicknameCommand(8, "User 8", "valid8");
    	assertNull("Null nickname if user deregisters", model.getNickname(8));
    	
    	//can't invite a deregistered user to a channel 
    	model.createChannel("TestChannelOwnerIs9", model.getNickname(9));
    	Command command3 = new InviteCommand(9, model.getNickname(9), "TestChannelOwnerIs9", "valid8");
    	Broadcast expected = Broadcast.error(command3, ServerError.NO_SUCH_USER);
    	Broadcast actual = command3.updateServerModel(model);
    	assertEquals("Broadcast deregistered user case", expected, actual);
    }
    
    @Test
    public void testJoinAndLeaveCases() {
    	model.registerUser(9);
    	model.registerUser(10);
    	model.registerUser(11);
    	//owner is 9
    	model.createChannel("TestChannelOwnerIs9", model.getNickname(9));
    	//Add users 10 and 11 twice 
    	Channel chan = model.getParticularChannel("TestChannelOwnerIs9");
    	model.joinChannel(chan, model.getNickname(10));
    	model.joinChannel(chan, model.getNickname(11));
    	model.joinChannel(chan, model.getNickname(10));
    	model.joinChannel(chan, model.getNickname(11));
    	Collection<String> usersInTestChan = model.getUsersInChannel("TestChannelOwnerIs9");
    	assertEquals("Users in channel with same users joining twice", 3, usersInTestChan.size());
    	
    	//Test when user 10 leaves
    	Command command = new LeaveCommand(10, model.getNickname(10), "TestChannelOwnerIs9");
    	command.updateServerModel(model);
    	Collection<String> usersInTestChan2 = model.getUsersInChannel("TestChannelOwnerIs9");
    	assertEquals("Users in channel when user 10 leaves", 2, usersInTestChan2.size());
    	
    }
    
    @Test
    public void differentRegisterUserCases() {
    	
    	model.registerUser(1);
    	model.registerUser(2); 
    	model.registerUser(3);
    	Command command = new NicknameCommand(1, "User 1", "valid1");
    	
    	//valid name case 
    	Broadcast expected = Broadcast.okay(command, Collections.singleton("User 1"));
    	Broadcast actual = command.updateServerModel(model);
    	assertEquals("Valid name", expected, actual); 
    	
    	//tests getNickname function 
    	
    	assertEquals("get nickname of user with ID 1 ", "valid1", model.getNickname(1));
    	
    	//tests getRegisteredUsers function 
    	Collection<String> registeredUsers = model.getRegisteredUsers();
    	assertEquals("Registered users total quantity", 3, registeredUsers.size());
    	
    	//if user 2 tries to register with already existing nickname
    	Command command2 = new NicknameCommand(2, "User 2", "valid1");
    	Broadcast expected2 = Broadcast.error(command2, ServerError.NAME_ALREADY_IN_USE);
    	Broadcast actual2 = command2.updateServerModel(model);
    	assertEquals("User 2 can't register with existing nickanme", expected, actual);
    	//user 2 still has same nickname 
    	String user2NameInSystem = model.getNickname(2);
    	assertEquals("User 2 still has same nickname", user2NameInSystem, model.getNickname(2)); 
    			
    }
    
    //Test cases with other users in channel function
    @Test
    public void otherUsersInChannel() {
    	model.registerUser(4); 
    	model.registerUser(5);
    	model.registerUser(6);
    	//Create channel with users 4, 5, 6; 4 as owner
    	model.createChannel("TestChannelforUsersInChannel", model.getNickname(4));
    	Channel chan = model.getParticularChannel("TestChannelforUsersInChannel");
    	//add users 5 and 6
    	//tests JoinChannel Command
    	model.joinChannel(chan, model.getNickname(5));
    	model.joinChannel(chan, model.getNickname(6));
    	//create collection of friends of user 4 
    	Collection <String> friendsOf4 = model.getOtherUsersInChannel(model.getNickname(4));
    	assertEquals("Quantity of friends of 4", 2, friendsOf4.size());
    	//Tests for friend identity 
    	assertTrue("user 5 is friend of user 4", friendsOf4.contains(model.getNickname(5)));
    	assertTrue("user 6 is friend of user 4", friendsOf4.contains(model.getNickname(6)));
    	assertFalse("user 4 is NOT friend of user 4", friendsOf4.contains(model.getNickname(4)));
    	
    }
    
    @Test
    public void testMessagesChannelTests() {
    	model.registerUser(15);
    	model.registerUser(16);
    	model.registerUser(17);
    	model.registerUser(18);
    	model.createChannel("TestChannelOwneris15", model.getNickname(15));
    	model.createChannel("TestChannel2", model.getNickname(15));
    	Channel chan = model.getParticularChannel("TestChannelOwneris15");
    	Channel chan2 = model.getParticularChannel("TestChannel2");
    	model.joinChannel(chan, model.getNickname(16));
    	model.joinChannel(chan, model.getNickname(17));
    	Command command = new MessageCommand(18, model.getNickname(18), "TestChannelOwneris15", "abc");
    	Broadcast expected = Broadcast.error(command, ServerError.USER_NOT_IN_CHANNEL);
    	Broadcast actual = command.updateServerModel(model);
    	assertEquals("message from user not in channel", expected, actual);
    	

    }
}
