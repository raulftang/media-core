/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.connection;

import java.io.IOException;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.MyTestEndpoint;
import static org.junit.Assert.*;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author kulikov
 */
public class BaseConnectionFSM_FR_Test {

    //clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    //endpoint and connection
    private BaseConnection connection;
    private MyTestEndpoint endpoint;

    //RTP
    private RTPManager rtpManager;

    private volatile int failureRate;
    private String hint;

    private Random rnd = new Random();

    public BaseConnectionFSM_FR_Test() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, IOException {    	
        ConnectionState.OPEN.setTimeout(5);
        ConnectionState.HALF_OPEN.setTimeout(5);

        //use default clock
        clock = new DefaultClock();
        
        //create single thread scheduler 
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        rtpManager = new RTPManager(new UdpManager(scheduler));
        rtpManager.setBindAddress("127.0.0.1");
        rtpManager.setScheduler(scheduler);
        
        //assign scheduler to the endpoint
        endpoint = new MyTestEndpoint("test");
        endpoint.setScheduler(scheduler);
        endpoint.setRtpManager(rtpManager);        
        endpoint.start();    	
    }

    @After
    public void tearDown() {
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
        }
        endpoint.deleteAllConnections();
        endpoint.stop();
        scheduler.stop();
        
    }

    /**
     * Simulates connection behaivior.
     * 
     * Any expection or missmatch detected breakes test execution.
     *  
     * @throws Exception
     */
    private void doTestCreate() throws Exception {
        //step #1: create connection;
        connection = (BaseConnection) endpoint.createConnection(ConnectionType.LOCAL);

        //step #2: check state, expected state is NULL;
//        if (connection.getState() != ConnectionState.NULL) {
//            System.out.println("Created connection in not NULL state");
//            failureRate++;
//            return;
//        }

        //step #3: shift to HALF_OPEN state
//        System.out.println("Creating connection");
//        connection.bind();

        //step #4: wait to allow transition
        Thread.sleep(1000);
        if (connection.getState() != ConnectionState.HALF_OPEN) {
            System.out.println("Bound connection in state: " + connection.getState());
            failureRate++;
            return;
        }

        //step #5: generate next action.
        boolean timeout = rnd.nextBoolean();

        //step #6: follow to action
        if (timeout) {
            doTestTimeout();
        } else {
            doTestOpen();
        }
    }

    private void doTestTimeout() throws Exception {
        //step #1: wait for timeout
        Thread.sleep(7000);

        //step #2: check state
        if (connection.getState() != ConnectionState.NULL) {
            System.out.println("Timeed out connection in state: " + connection.getState());
            failureRate++;
        }
    }

    private void doTestOpen() throws Exception {
        //step #1: shift to OPEN state
//        System.out.println("Opening connection");
        connection.join();
        Thread.sleep(1000);

        //step #2: check state
        if (connection.getState() != ConnectionState.OPEN) {
            System.out.println("Opened connection in state: " + connection.getState());
            failureRate++;
            return;
        }

        //step #3: generate next action.
        boolean timeout = rnd.nextBoolean();

        //step #4: follow to action
        if (timeout) {
            doTestTimeout();
        } else {
            doTestClose();
        }
    }

    private void doTestClose() throws InterruptedException {
        //step #1: shift to OPEN state
//        System.out.println("Closing connection");
        connection.close();
        Thread.sleep(1000);

        //step #2: check state
        if (connection.getState() != ConnectionState.NULL) {
            System.out.println("Closed connection in state: " + connection.getState());
            failureRate++;
        }
    }

    @Test
    public void testFailureRate() throws Exception {
        int N = 1;
        for (int i = 0; i < N; i++) {
            System.out.println("Run test #" + i + ": failure rate = " + (double)failureRate/N);
            doTestCreate();
            assertTrue("Failure rate too big", failureRate == 0);
        }

    }

    @Test
    public void testNothing() {
        //unit tests without tests is not working
    }
}