package protocol;

import java.net.InetAddress;

/**
 * Created by Greg on 9/6/16.
 * Chat participant object
 */

public class ChatParticipant {
        public InetAddress IP;
        public int port;

        public String toString(){
                return IP.getHostAddress() + " " + port;
        }
}
