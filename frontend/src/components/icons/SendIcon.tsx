import React from 'react';
import { Icon, IconProps } from './Icon';

export const SendIcon: React.FC<IconProps> = (props) => (
  <Icon {...props}>
    <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
  </Icon>
);
