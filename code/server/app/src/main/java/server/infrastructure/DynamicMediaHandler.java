package server.infrastructure;

/**
 * Interface for serving dynamic media content (e.g., images).
 *
 * Implementations of this interface define how media resources are stored,
 * retrieved, and served to clients using an ID-based lookup.
 *
 * E.g. {@link FileDynamicMediaHandler} maps long IDs to media files
 * stored in a nested folder structure under a root path.
 */
public interface DynamicMediaHandler {

    /**
     * Retrieves the byte content of a media file associated with a given ID.
     *
     * @param id The unique identifier for the media file.
     * @return A byte array representing the media content (e.g., image data).
     * @throws Exception if the media cannot be found, read, or accessed.
     */
    byte[] get(long id);

    /**
     * Deletes the media file associated with the given ID.
     *
     * @param id
     */
    void delete(long id);

    /**
     * Adds a new media file to the system.
     *
     * @param data Byte array of the file to add.
     * @return Unique ID assigned to the stored media file.
     */
    long add(byte[] data);
}
