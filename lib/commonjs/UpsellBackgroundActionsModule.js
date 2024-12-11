"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.nativeEventEmitter = exports.UpsellBackgroundActions = void 0;
var _reactNative = require("react-native");
const {
  UpsellBackgroundActions
} = _reactNative.NativeModules;
exports.UpsellBackgroundActions = UpsellBackgroundActions;
const nativeEventEmitter = exports.nativeEventEmitter = new _reactNative.NativeEventEmitter(UpsellBackgroundActions);
//# sourceMappingURL=UpsellBackgroundActionsModule.js.map