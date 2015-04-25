package challengetask.group02.helpers;

import challengetask.group02.fsstructure.Directory;
import challengetask.group02.fsstructure.Entry;

import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerDHT;

import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

import java.io.IOException;
import java.util.Map;

/**
 * Created by anvar on 25/04/15.
 */
public class DHTPutGetHelper {
    PeerDHT peer;

    public DHTPutGetHelper(PeerDHT peer) {
        this.peer = peer;
    }

    public int addNewEntry(Directory parentDir, Entry child){
        //first have to update parent
        try {
            parentDir.addChild(child.getEntryName(), child.getID(), child.getType());
            put(parentDir);
            //if previous was successfull, can put the child itself
            put(child);
            //if this was successful either, then all is ok.
        } catch (Exception e){

        }
        return 0;
    }

    public int updateEntryName(Directory parent, Entry entry, String newName) {

        try{
            //Modify parent
            parent.renameChild(entry.getEntryName(), newName);
            put(parent);
            //if previous was successfull, can put the child itself
            entry.setEntryName(newName);
            put(entry);
            //if this was successful either, then all is ok.
        } catch (Exception e){

        }

        return 0;
    }

    private void put(Entry entry) throws IOException {
        FuturePut futurePut = peer.put(entry.getID()).data(new Data(entry)).start();
        futurePut.awaitUninterruptibly();
    }


    public int moveEntry(Directory newParent, Directory oldParent, Entry entry, String newName) {
        newParent.addChild(newName, entry.getID(), entry.getType());
        oldParent.removeChild(entry.getEntryName());
        entry.setEntryName(newName);
        entry.setParentID(newParent.getID());

        try{
            //trying to update new parent first
            put(newParent);
            //if ok, have to modify the old parent
            put(oldParent);
            //if ok, can finally update the entry itself
            put(entry);
        } catch (Exception e){

        }
        return 0;
    }
}