import { NativeModules, Platform, AppRegistry } from 'react-native';

const { UpsellBackgroundActions } = NativeModules;

class UpsellBackgroundService {
  _runnedTasks: number;
  _stopTask: any;
  _isRunning: boolean;
  _currentOptions: any;
  constructor() {
    /** @private */
    this._runnedTasks = 0;
    /** @private @type {(arg0?: any) => void} */
    this._stopTask = () => {};
    /** @private */
    this._isRunning = false;
    /** @private @type {BackgroundTaskOptions} */
    this._currentOptions;
  }

  async updateNotification(taskData: any) {
    if (Platform.OS !== 'android') return;
    if (!this.isRunning())
      throw new Error(
        'A BackgroundAction must be running before updating the notification'
      );
    this._currentOptions = this._normalizeOptions({
      ...this._currentOptions,
      ...taskData,
    });
    await UpsellBackgroundActions.updateNotification(this._currentOptions);
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
  async setCallBack(callback: any) {
    UpsellBackgroundActions.setCallBack(callback);
  }

  async start(task: any, options: { parameters: any }) {
    this._runnedTasks++;
    this._currentOptions = this._normalizeOptions(options);
    const finalTask = this._generateTask(task, options.parameters);
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask(
        this._currentOptions.taskName,
        () => finalTask
      );
      await UpsellBackgroundActions.start(this._currentOptions);
      this._isRunning = true;
    } else {
      await UpsellBackgroundActions.start(this._currentOptions);
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
  _generateTask(task: (arg0: any) => Promise<any>, parameters: any) {
    // eslint-disable-next-line consistent-this
    const self = this;
    return async () => {
      await new Promise((resolve) => {
        self._stopTask = resolve;
        task(parameters).then(() => self.stop());
      });
    };
  }

  _normalizeOptions(options: any) {
    return {
      taskName: options.taskName + this._runnedTasks,
      taskTitle: options.taskTitle,
      taskDesc: options.taskDesc,
      taskIcon: { ...options.taskIcon },
      color: options.color || '#ffffff',
      linkingURI: options.linkingURI,
      progressBar: options.progressBar,
    };
  }

  async stop() {
    this._stopTask();
    await UpsellBackgroundActions.stop();
    this._isRunning = false;
  }
  async sendStopBroadcast() {
    this._stopTask();
    await UpsellBackgroundActions.sendStopBroadcast();
    this._isRunning = false;
  }
  async setAlarm(mili: number) {
    return await UpsellBackgroundActions.setAlarm(mili);
  }
  async isBackgroundServiceRunning() {
    return await UpsellBackgroundActions.isBackgroundServiceRunning();
  }
  async checkScheduleExactAlarmPermission() {
    return await UpsellBackgroundActions.checkScheduleExactAlarmPermission();
  }
  async requestExactAlarmPermission() {
    return await UpsellBackgroundActions.requestExactAlarmPermission();
  }
  async listRunningServices() {
    return await UpsellBackgroundActions.listRunningServices();
  }
  async stopAlarm() {
    return await UpsellBackgroundActions.stopAlarm();
  }
  async setIsBackgroundServiceRunning(value: boolean) {
    return await UpsellBackgroundActions.setIsBackgroundServiceRunning(value);
  }
  async lock() {
    return await UpsellBackgroundActions.lock();
  }
  async unlock() {
    return await UpsellBackgroundActions.unlock();
  }
  async isItSafeToStopAlarm() {
    return await UpsellBackgroundActions.getIsItSafeToStopAlarm();
  }
}
const BackgroundService = new UpsellBackgroundService();
export default BackgroundService;
