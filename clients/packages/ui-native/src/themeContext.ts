import { nativeTheme, type NativeTheme } from '@mysms/tokens';
import { createContext, useContext } from 'react';

export const ThemeContext = createContext<NativeTheme>(nativeTheme('light'));

export const useTheme = (): NativeTheme => useContext(ThemeContext);
