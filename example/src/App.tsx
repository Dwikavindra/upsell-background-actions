import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, Button } from 'react-native';
import BackgroundService from 'upsell-background-actions';

export default function App() {
  const [result, setResult] = useState<number | undefined>();
  const sleep = (time: any) =>
    new Promise<void>((resolve) => setTimeout(() => resolve(), time));

  // You can do anything in your task such as network requests, timers and so on,
  // as long as it doesn't touch UI. Once your task completes (i.e. the promise is resolved),
  // React Native will go into "paused" mode (unless there are other tasks running,
  // or there is a foreground app).
  const veryIntensiveTask = async (taskDataArguments: any) => {
    // Example of an infinite loop task
    const { delay } = taskDataArguments;
    console.log('Here in intensive task');

    await BackgroundService.lock();
    await BackgroundService.setAlarm(10000);
    await new Promise(async (resolve) => {
      for (
        let i = 0;
        await BackgroundService.isBackgroundServiceRunning();
        i++
      ) {
        console.log(
          'This is await isBackgroundServiceRunning',
          await BackgroundService.isBackgroundServiceRunning()
        );
        console.log('Very Intensive Task Started', i);
        await sleep(delay);
      }
      console.log('IsBackgroundServiceRunningis false');
      try {
        console.log(
          'This is is list await ',
          await BackgroundService.listRunningServices()
        );
        while ((await BackgroundService.listRunningServices()) !== '[]') {
          console.log('Awaiting for running Services to be 0');
          await BackgroundService.sendStopBroadcast();
          await sleep(1000);
        }
        await BackgroundService.unlock();
        console.log('Passed lock');
      } catch (error) {
        console.log(error);
      }
    });
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
            console.log('Here');
            await BackgroundService.start(veryIntensiveTask, options);
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
        title="Stop Task "
        onPress={async () => {
          try {
            await BackgroundService.setIsBackgroundServiceRunning(false);

            while ((await BackgroundService.listRunningServices()) !== '[]') {
              await BackgroundService.sendStopBroadcast();
              await BackgroundService.setIsBackgroundServiceRunning(false);
            }

            await BackgroundService.stopAlarm();
            console.log('Passed sendStopBroadcast');
          } catch (error) {
            console.log('Error from stop task', error);
          }
        }}
      />
      <Text>Result: {result}</Text>
    </View>
  );
}

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
