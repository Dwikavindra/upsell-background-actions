import { NativeEventEmitter, NativeModules } from 'react-native';

const { UpsellBackgroundActions } = NativeModules;

const nativeEventEmitter = new NativeEventEmitter(UpsellBackgroundActions);

export { UpsellBackgroundActions, nativeEventEmitter };
