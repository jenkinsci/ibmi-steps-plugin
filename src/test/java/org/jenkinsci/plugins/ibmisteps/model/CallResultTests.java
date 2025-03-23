package org.jenkinsci.plugins.ibmisteps.model;

import com.ibm.as400.access.AS400Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CallResultTests {
	@Test
	void callResultTest(){
		final CallResult callResult = new CallResult(true, new AS400Message[0]);
		assertTrue(callResult.isSuccessful());
		assertTrue(callResult.getMessages().isEmpty());
		assertNull(callResult.getLastMessage());

		final List<AS400Message> messages = new ArrayList<>();
		messages.add(mock(AS400Message.class));
		messages.add(mock(AS400Message.class));
		AS400Message lastMessages = mock(AS400Message.class);
		messages.add(lastMessages);

		final CallResult callResult2 = new CallResult(false, messages.toArray(new AS400Message[0]));
		assertFalse(callResult2.isSuccessful());
		assertFalse(callResult2.getMessages().isEmpty());
		assertEquals(lastMessages, callResult2.getLastMessage());
	}
}
