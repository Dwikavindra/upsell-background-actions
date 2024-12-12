"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _reactNative = require("react-native");
var _index = require("./index.js");
var _eventemitter = _interopRequireDefault(require("eventemitter3"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
/**
 * @typedef {{taskName: string,
 *            taskTitle: string,
 *            taskDesc: string,
 *            taskIcon: {name: string, type: string, package?: string},
 *            color?: string
 *            linkingURI?: string,
 *            progressBar?: {max: number, value: number, indeterminate?: boolean}
 *            }} BackgroundTaskOptions
 * @extends EventEmitter<'expiration',any>
 */
class BackgroundServer extends _eventemitter.default {
  constructor() {
    super();
    /** @private */
    this._runnedTasks = 0;
    /** @private @type {(arg0?: any) => void} */
    this._stopTask = () => {};
    /** @private */
    this._isRunning = false;
    /** @private @type {BackgroundTaskOptions} */
    this._currentOptions;
    this._addListeners();
  }

  /**
   * @private
   */
  _addListeners() {
    _index.nativeEventEmitter.addListener('expiration', () => this.emit('expiration'));
  }

  /**
   * **ANDROID ONLY**
   *
   * Updates the task notification.
   *
   * *On iOS this method will return immediately*
   *
   * @param {{taskTitle?: string,
   *          taskDesc?: string,
   *          taskIcon?: {name: string, type: string, package?: string},
   *          color?: string,
   *          linkingURI?: string,
   *          progressBar?: {max: number, value: number, indeterminate?: boolean}}} taskData
   */
  async updateNotification(taskData) {
    if (_reactNative.Platform.OS !== 'android') return;
    if (!this.isRunning()) throw new Error('A BackgroundAction must be running before updating the notification');
    this._currentOptions = this._normalizeOptions({
      ...this._currentOptions,
      ...taskData
    });
    await _index.UpsellBackgroundActions.updateNotification(this._currentOptions);
  }

  /**
   * Returns if the current background task is running.
   *
   * It returns `true` if `start()` has been called and the task has not finished.
   *
   * It returns `false` if `stop()` has been called, **even if the task has not finished**.
   */
  isRunning() {
    return this._isRunning;
  }
  /**
   * @argument {()=>{}} callback
   * @returns {Promise<void>}
   */
  async setCallBack(callback) {
    _index.UpsellBackgroundActions.setCallBack(callback);
  }

  /**
   * @template T
   *
   * @param {(taskData?: T) => Promise<void>} task
   * @param {BackgroundTaskOptions & {parameters?: T}} options
   * @param  {number} triggerTime when will the alarm fire to trigger the restart callback
   * @returns {Promise<void>}
   */
  async start(task, options, triggerTime) {
    console.log('Here passed register');
    this._runnedTasks++;
    this._currentOptions = this._normalizeOptions(options);
    const finalTask = this._generateTask(task, options.parameters);
    if (_reactNative.Platform.OS === 'android') {
      _reactNative.AppRegistry.registerHeadlessTask(this._currentOptions.taskName, () => finalTask);
      await _index.UpsellBackgroundActions.start(this._currentOptions, triggerTime);
      this._isRunning = true;
    } else {
      await _index.UpsellBackgroundActions.start(this._currentOptions, triggerTime);
      this._isRunning = true;
      finalTask();
    }
  }

  /**
   * @private
   * @template T
   * @param {(taskData?: T) => Promise<void>} task
   * @param {T} [parameters]
   */
  _generateTask(task, parameters) {
    const self = this;
    return async () => {
      await new Promise(resolve => {
        self._stopTask = resolve;
        task(parameters).then(() => self.stop());
      });
    };
  }

  /**
   * @private
   * @param {BackgroundTaskOptions} options
   */
  _normalizeOptions(options) {
    return {
      taskName: options.taskName + this._runnedTasks,
      taskTitle: options.taskTitle,
      taskDesc: options.taskDesc,
      taskIcon: {
        ...options.taskIcon
      },
      color: options.color || '#ffffff',
      linkingURI: options.linkingURI,
      progressBar: options.progressBar
    };
  }

  /**
   * Stops the background task.
   *
   * @returns {Promise<void>}
   */
  async stop() {
    this._stopTask();
    await _index.UpsellBackgroundActions.stop();
    this._isRunning = false;
  }
  async sendStopBroadcast() {
    this._stopTask();
    await _index.UpsellBackgroundActions.sendStopBroadcast();
    this._isRunning = false;
  }
  async isBackgroundServiceRunning() {
    return await _index.UpsellBackgroundActions.isBackgroundServiceRunning();
  }
  async checkScheduleExactAlarmPermission() {
    return await _index.UpsellBackgroundActions.checkScheduleExactAlarmPermission();
  }
  async requestExactAlarmPermission() {
    return await _index.UpsellBackgroundActions.requestExactAlarmPermission();
  }
  async listRunningServices() {
    return await _index.UpsellBackgroundActions.listRunningServices();
  }
  async stopAlarm() {
    return await _index.UpsellBackgroundActions.stopAlarm();
  }
}
const BackgroundService = new BackgroundServer();
var _default = exports.default = BackgroundService;
//# sourceMappingURL=index.js.map