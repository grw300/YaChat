package protocol;

import java.io.*;
import java.net.*;

/**
 * Created by Greg on 9/5/16.
 */

interface Context {
    BufferedReader buffer();
    State state();
    void state(State state);
}

interface State {
    /**
     * @return true to keep processing, false to read more data.
     */
    boolean process(Context context);
}

enum YaProtocol implements State {
    HELO {
        public boolean process(Context context) {
            try {
                String message = context.buffer().readLine();

            } catch (IOException ioexception){

            }

            // read HELO message
            return true;
        }
    }, MESG {
        public boolean process(Context context) {
            try {
                String message = context.buffer().readLine();

            } catch (IOException ioexception){

            }

            // read HELO message
            return true;
        }
    }, EXIT {
        public boolean process(Context context) {
            return true;
        }
    }, ACPT {
        public boolean process(Context context) {
            return true;
        }
    }, RJCT {
        public boolean process(Context context) {
            return true;
        }
    }, JOIN {
        public boolean process(Context context) {
            return true;
        }
    }
}

//public class YaProtocol {
//    /** Use these for comments **/
//
//}
