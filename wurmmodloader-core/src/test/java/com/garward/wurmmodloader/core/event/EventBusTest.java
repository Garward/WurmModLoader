package com.garward.wurmmodloader.core.event;

import com.garward.wurmmodloader.api.events.base.Event;
import com.garward.wurmmodloader.api.events.base.EventPriority;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.server.ServerStoppingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for the EventBus.
 */
public class EventBusTest {

    private EventBus eventBus;

    @Before
    public void setUp() {
        eventBus = new EventBus();
    }

    @After
    public void tearDown() {
        if (eventBus != null) {
            eventBus.unregisterAll();
        }
    }

    @Test
    public void testRegisterAndPostEvent() {
        TestListener listener = new TestListener();
        eventBus.register(listener);

        ServerStartedEvent event = new ServerStartedEvent();
        eventBus.post(event);

        assertEquals(1, listener.eventsReceived.size());
        assertTrue(listener.eventsReceived.get(0) instanceof ServerStartedEvent);
    }

    @Test
    public void testMultipleListeners() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();

        eventBus.register(listener1);
        eventBus.register(listener2);

        ServerStartedEvent event = new ServerStartedEvent();
        eventBus.post(event);

        assertEquals(1, listener1.eventsReceived.size());
        assertEquals(1, listener2.eventsReceived.size());
    }

    @Test
    public void testPriorityOrdering() {
        List<String> callOrder = new ArrayList<>();

        Object highPriorityListener = new Object() {
            @SubscribeEvent(priority = EventPriority.HIGH)
            public void onEvent(ServerStartedEvent event) {
                callOrder.add("HIGH");
            }
        };

        Object normalPriorityListener = new Object() {
            @SubscribeEvent(priority = EventPriority.NORMAL)
            public void onEvent(ServerStartedEvent event) {
                callOrder.add("NORMAL");
            }
        };

        Object lowPriorityListener = new Object() {
            @SubscribeEvent(priority = EventPriority.LOW)
            public void onEvent(ServerStartedEvent event) {
                callOrder.add("LOW");
            }
        };

        // Register in non-priority order to ensure sorting works
        eventBus.register(lowPriorityListener);
        eventBus.register(highPriorityListener);
        eventBus.register(normalPriorityListener);

        eventBus.post(new ServerStartedEvent());

        assertEquals(3, callOrder.size());
        assertEquals("HIGH", callOrder.get(0));
        assertEquals("NORMAL", callOrder.get(1));
        assertEquals("LOW", callOrder.get(2));
    }

    @Test
    public void testEventCancellation() {
        TestCancellableEvent event = new TestCancellableEvent();

        Object cancellingListener = new Object() {
            @SubscribeEvent(priority = EventPriority.HIGH)
            public void onEvent(TestCancellableEvent evt) {
                evt.setCancelled(true);
            }
        };

        TestListener normalListener = new TestListener();

        eventBus.register(cancellingListener);
        eventBus.register(normalListener);

        eventBus.post(event);

        assertTrue(event.isCancelled());
        // Normal listener should not receive the cancelled event
        assertEquals(0, normalListener.eventsReceived.size());
    }

    @Test
    public void testReceiveCancelled() {
        TestCancellableEvent event = new TestCancellableEvent();
        List<String> callOrder = new ArrayList<>();

        Object cancellingListener = new Object() {
            @SubscribeEvent(priority = EventPriority.HIGH)
            public void onEvent(TestCancellableEvent evt) {
                evt.setCancelled(true);
                callOrder.add("CANCEL");
            }
        };

        Object receiveCancelledListener = new Object() {
            @SubscribeEvent(receiveCancelled = true)
            public void onEvent(TestCancellableEvent evt) {
                callOrder.add("RECEIVE_CANCELLED");
            }
        };

        Object normalListener = new Object() {
            @SubscribeEvent
            public void onEvent(TestCancellableEvent evt) {
                callOrder.add("NORMAL");
            }
        };

        eventBus.register(cancellingListener);
        eventBus.register(receiveCancelledListener);
        eventBus.register(normalListener);

        eventBus.post(event);

        assertTrue(event.isCancelled());
        assertEquals(2, callOrder.size());
        assertEquals("CANCEL", callOrder.get(0));
        assertEquals("RECEIVE_CANCELLED", callOrder.get(1));
        // NORMAL should not be in the list
    }

    @Test
    public void testUnregister() {
        TestListener listener = new TestListener();
        eventBus.register(listener);

        eventBus.post(new ServerStartedEvent());
        assertEquals(1, listener.eventsReceived.size());

        eventBus.unregister(listener);

        eventBus.post(new ServerStartedEvent());
        // Should still be 1, not 2
        assertEquals(1, listener.eventsReceived.size());
    }

    @Test
    public void testNoSubscribersForEvent() {
        // Should not throw exception when posting event with no subscribers
        eventBus.post(new ServerStartedEvent());
        assertEquals(0, eventBus.getSubscriberCount(ServerStartedEvent.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNull() {
        eventBus.register(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPostNull() {
        eventBus.post(null);
    }

    @Test
    public void testInvalidSubscriberMethod() {
        Object invalidListener = new Object() {
            @SubscribeEvent
            public void noParameters() {
                // Invalid - no parameters
            }

            @SubscribeEvent
            public void tooManyParameters(Event e1, Event e2) {
                // Invalid - too many parameters
            }

            @SubscribeEvent
            public int wrongReturnType(Event e) {
                // Invalid - must return void
                return 0;
            }
        };

        try {
            eventBus.register(invalidListener);
            fail("Should have thrown IllegalArgumentException for invalid subscriber methods");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testGetListenerCount() {
        assertEquals(0, eventBus.getListenerCount());

        TestListener listener1 = new TestListener();
        eventBus.register(listener1);
        assertEquals(1, eventBus.getListenerCount());

        TestListener listener2 = new TestListener();
        eventBus.register(listener2);
        assertEquals(2, eventBus.getListenerCount());

        eventBus.unregister(listener1);
        assertEquals(1, eventBus.getListenerCount());
    }

    @Test
    public void testGetSubscriberCount() {
        TestListener listener = new TestListener();
        eventBus.register(listener);

        assertEquals(1, eventBus.getSubscriberCount(ServerStartedEvent.class));
        assertEquals(1, eventBus.getSubscriberCount(ServerStoppingEvent.class));
        assertEquals(0, eventBus.getSubscriberCount(TestCancellableEvent.class));
    }

    // Test helper classes

    static class TestListener {
        List<Event> eventsReceived = new ArrayList<>();

        @SubscribeEvent
        public void onServerStarted(ServerStartedEvent event) {
            eventsReceived.add(event);
        }

        @SubscribeEvent
        public void onServerStopping(ServerStoppingEvent event) {
            eventsReceived.add(event);
        }
    }

    static class TestCancellableEvent extends Event {
        public TestCancellableEvent() {
            super(true); // Cancellable
        }
    }
}
