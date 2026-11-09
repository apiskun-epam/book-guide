import React from 'react';
import { Icon, IconProps } from './Icon';

export const LibraryIcon: React.FC<IconProps> = (props) => (
  <Icon {...props}>
    <path d="M4 4v16M9 4v16M14 6l4.5 14M14 6l-1 .3" />
  </Icon>
);
