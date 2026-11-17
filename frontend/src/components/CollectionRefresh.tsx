import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

interface CollectionRefreshValue {
  /** Changes every time a book is added from anywhere (e.g. the top bar search). */
  version: number;
  notifyBookAdded: () => void;
}

const CollectionRefreshContext = createContext<CollectionRefreshValue>({
  version: 0,
  notifyBookAdded: () => undefined,
});

/** Lets the shared top bar tell the collection page to reload after a book is added. */
export const CollectionRefreshProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [version, setVersion] = useState(0);
  const notifyBookAdded = useCallback(() => setVersion((v) => v + 1), []);
  const value = useMemo(() => ({ version, notifyBookAdded }), [version, notifyBookAdded]);
  return <CollectionRefreshContext.Provider value={value}>{children}</CollectionRefreshContext.Provider>;
};

export const useCollectionRefresh = (): CollectionRefreshValue => useContext(CollectionRefreshContext);
