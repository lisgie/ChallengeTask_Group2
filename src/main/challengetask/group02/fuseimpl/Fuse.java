package challengetask.group02.fuseimpl;

import challengetask.group02.controllers.*;
import challengetask.group02.controllers.exceptions.*;
import net.fusejna.*;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.types.TypeMode;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.util.Objects;

/*
Fuse interface implementation
requires the mount point and the controller for the constructor.

Information about FUSE can be found in the project wiki,
to know more about fuse-jna go to https://github.com/EtiennePerot/fuse-jna
 */

public class Fuse extends FuseFilesystemAdapterAssumeImplemented {

    private final String path;
    private ControllerContext controller;

    public Fuse(ControllerContext controller, String path) {
        this.controller = controller;
        this.path = path;
    }

    public void run(Logger logger) throws FuseException {
        try {
            this.log(false).mount(path);
            //this.log(logger).mount(path);
        } catch (FuseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getattr(final String path, final StructStat.StatWrapper stat) {
        try {
            controller.updateFileMetaData(path, stat);
            return 0;
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return -ErrorCodes.ENOENT();
    }

    @Override
    public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final StructFuseFileInfo.FileInfoWrapper info) {
        // Compute substring that we are being asked to read
        byte[] s = new byte[0];
        try {
            s = controller.readFile(path, size, offset);
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } catch (CRCException e) {
        	e.printStackTrace();
        }
        buffer.put(s);
        return s.length;
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler) {
        try {
            for (String child : controller.readDir(path)) {
                filler.add(child);
            }
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int mkdir(final String path, final TypeMode.ModeWrapper mode) {
        try {
            controller.createDir(path);
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int create(final String path, final TypeMode.ModeWrapper mode, final StructFuseFileInfo.FileInfoWrapper info) {
        try {
            controller.createFile(path);
            //mode.setMode(TypeMode.NodeType.DIRECTORY);
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int rename(final String path, final String newName) {
        try {
            controller.rename(path, newName);
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int rmdir(final String path) {
        try {
            controller.deleteDirectory(path);
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int unlink(final String path) {
        try {
            controller.deleteFile(path);
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int write(final String path, final ByteBuffer buf, final long bufSize, final long writeOffset,
                     final StructFuseFileInfo.FileInfoWrapper wrapper) {
        try {
            return controller.writeFile(path, buf, bufSize, writeOffset);
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        //return ((MemoryFile) p).write(buf, bufSize, writeOffset);

        return 0;
    }

    @Override
    public int flush(final String path, final FileInfoWrapper info) {

        //introduced for the locking mechanism
        //flush is only used in context with file
        try {
            controller.whenFileClosed(path);
        } catch (FsException e) {
            return getErrorCode(e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return 0;
    }


    /**
     * This method returns the right FUSE error code, depending on what type the FsException is.
     *
     * @param e FsException
     * @return int Fuse Error code
     */
    private int getErrorCode(FsException e) {
        if (Objects.equals(e.getClass().getName(), NotADirectoryException.class.getName())) {
            return -ErrorCodes.ENOTDIR();
        }
        if (Objects.equals(e.getClass().getName(), DirectoryNotEmptyException.class.getName())) {
            return -ErrorCodes.ENOTEMPTY();
        }
        if (Objects.equals(e.getClass().getName(), BusyException.class.getName())) {
            //TODO is this the right error code?
            return -ErrorCodes.EBUSY();
        }
        if (Objects.equals(e.getClass().getName(), NoSuchFileOrDirectoryException.class.getName())) {
            return -ErrorCodes.ENOENT();
        }
        if (Objects.equals(e.getClass().getName(), NotAFileException.class.getName())) {
            return -ErrorCodes.EISDIR();
        } else {
        	return -1;
        }
    }
}
