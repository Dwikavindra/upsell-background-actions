import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, Button } from 'react-native';
import BackgroundService from 'upsell-background-actions';
import * as Sentry from '@sentry/react-native';

Sentry.init({
  dsn: 'https://3255bd38d055853e9b2a54d2c73af785@o4509191714701312.ingest.us.sentry.io/4509191719288832',
  // Adds more context data to events (IP address, cookies, user, etc.)
  // For more information, visit: https://docs.sentry.io/platforms/react-native/data-management/data-collected/
  sendDefaultPii: true,
});
function App() {
  const [result, setResult] = useState<number | undefined>();
  let process = 0;
  const sleep = (time: any) =>
    new Promise<void>((resolve) => setTimeout(() => resolve(), time));

  // You can do anything in your task such as network requests, timers and so on,
  // as long as it doesn't touch UI. Once your task completes (i.e. the promise is resolved),
  // React Native will go into "paused" mode (unless there are other tasks running,
  // or there is a foreground app).
  const STOP_ALARM_MAX_RETRIES = 6;
  const stopAlarm = async () => {
    let i = 0;
    while (i < STOP_ALARM_MAX_RETRIES) {
      try {
        await BackgroundService.stopAlarm();
      } catch (error) {
        const receivedError: Error = error as Error;
        if (receivedError.message.includes('Pending Intent not Found')) {
          console.log('This is error Pending Intent', receivedError);
        }
        if (receivedError.message.includes('Not Safe to stop Alarm')) {
          console.log('This is error', receivedError);
        }
      }
      i++;
    }
  };
  const veryIntensiveTask = async (taskDataArguments: any) => {
    // Example of an infinite loop task
    let isError = false;
    try {
      console.log('Here in intensive task');
      process = process + 1;
      await BackgroundService.lock();
      console.log('Passed lock');
      for (
        let i = 0;
        await BackgroundService.isBackgroundServiceRunning();
        i++
      ) {
        if (i === 10) {
          throw new Error('Testing Error for restart');
        }
        console.log(
          'This is await isBackgroundServiceRunning',
          await BackgroundService.isBackgroundServiceRunning()
        );
        console.log('Number', i);
        await sleep(1000);

        // asleep longer than waiting time
      }
    } catch (e) {
      console.log('Error from intensive', e);
      isError = true;
    } finally {
      console.log(
        'This is is list await ',
        await BackgroundService.listRunningServices()
      );
      if (isError === true) {
        await BackgroundService.sendStartServiceIntentInCatch();
        console.log('Passed sendstartServiceInCatch');
      }
      console.log('Passed while loop');
      await BackgroundService.unlock();
      console.log('Passed unlock');
      process = process - 1;
      console.log('Process after unlock', process);
    }
  };

  const options = {
    taskName: 'Example',
    taskTitle: 'ExampleTask title',
    taskDesc: 'ExampleTask description',
    taskIcon: {
      name: 'ic_launcher',
      type: 'mipmap',
    },
    color: '#ff00ff',
    linkingURI: 'yourSchemeHere://chat/jane', // See Deep Linking for more info
    parameters: {
      delay: 1000,
    },
  };
  // const restart1 = async () => {
  //   try {
  //     console.log('Here in restart 1');
  //     await BackgroundService.setIsBackgroundServiceRunning(false);
  //     await BackgroundService.sendStopBroadcast();
  //     await BackgroundService.stopAlarm();
  //     await sleep(5000);
  //     console.log(
  //       'This is running services',
  //       await BackgroundService.listRunningServices()
  //     );
  //     await BackgroundService.start(veryIntensiveTask, options, 30000);
  //   } catch (error) {
  //     console.log('Restart1 error', error);
  //   }
  // };

  return (
    <View style={styles.container}>
      <Button
        title="running services "
        onPress={async () => {
          console.log(await BackgroundService.listRunningServices());
        }}
      />
      <Button
        title="stopAlarm"
        onPress={async () => {
          await BackgroundService.stopAlarm();
        }}
      />
      <Button
        title="Start Task "
        onPress={async () => {
          try {
            BackgroundService.start(veryIntensiveTask, options, 5000);

            console.log('Here After');
          } catch (error) {
            console.log('This is error', error);
          }
        }}
      />
      <Button
        title="SetIsBackgroundServiceRunning False"
        onPress={async () => {
          try {
            console.log('Here');
            await BackgroundService.setIsBackgroundServiceRunning(false);
            console.log('Here After');
          } catch (error) {
            console.log('This is error', error);
          }
        }}
      />
      <Button
        title="Acquire AddPrinterSemaphore"
        onPress={async () => {
          try {
            console.log('Here Before lockAddPrinterSemaphore');
            await BackgroundService.lockAddPrinterSemaphore();
            console.log('Here After lockAddPrinterSemaphore');
          } catch (error) {
            console.log('This is error', error);
          }
        }}
      />
      <Button
        title="unlockAddPrinterSemaphorre"
        onPress={async () => {
          try {
            console.log('Here before unlock addPrinterSemaphore');
            await BackgroundService.unlockAddPrinterSemaphore();
            console.log('Here After unlockAddPrinterSemaphore');
          } catch (error) {
            console.log('This is error', error);
          }
        }}
      />
      <Button
        title="requestSchedulExactAlarmPermisssion"
        onPress={async () => {
          try {
            await BackgroundService.requestExactAlarmPermission();
          } catch (error) {
            console.log('This is error', error);
          }
        }}
      />
      <Button
        title="canScheduleExactAlarmPermission"
        onPress={async () => {
          try {
            const result =
              await BackgroundService.checkScheduleExactAlarmPermission();
            console.log('This is canScheduleExactAlarm', result);
          } catch (error) {
            console.log('This is error', error);
          }
        }}
      />
      <Button
        title="Stop Task "
        onPress={async () => {
          try {
            console.log('Here in stop task');
            try {
              await BackgroundService.stopAlarm();
            } catch (error) {
              console.log('error from stopAlarm', error);
            }

            console.log('Passed Stopped Alarm');

            await BackgroundService.setIsBackgroundServiceRunning(false);
            console.log('Passed setIsBackgroundServiceRunnign');
            await BackgroundService.sendStopBroadcast();
            console.log('sendStopBroadcast');
            await BackgroundService.unlock();
            console.log('Passed unlock here ');
            await BackgroundService.interruptQueuedThread();
            console.log('Pass intteruptThread');
            console.log('Passed sendStopBroadcast');
          } catch (error) {
            console.log('Error from stop task', error);
          }
        }}
      />
      <Button title="Test Sentry  " onPress={async () => {}} />
      <Text>Result: {result}</Text>
    </View>
  );
}
export default Sentry.wrap(App);
const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
