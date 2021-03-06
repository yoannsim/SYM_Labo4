package ch.heigvd.iict.sym_labo4.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.util.LogPrinter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Calendar;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.data.Data;

/**
 * @Class       : BleOperationsViewModel
 * @Author(s)   : Spinelli Isaia et Simonet Yoann
 * @Date        : 10.01.2020
 */
public class BleOperationsViewModel extends AndroidViewModel {


    private static final String TAG = BleOperationsViewModel.class.getSimpleName();

    private MySymBleManager ble = null;
    private BluetoothGatt mConnection = null;

    //live data - observer
    private final MutableLiveData<Boolean> mIsConnected = new MutableLiveData<>();
    public LiveData<Boolean> isConnected() {
        return mIsConnected;
    }

    private final MutableLiveData<Calendar> Calendrier = new MutableLiveData<>();
    public LiveData<Calendar> getdate() { return Calendrier; }

    private final MutableLiveData<Float> temperatureCelsius = new MutableLiveData<>();
    public LiveData<Float> getTemperature() { return temperatureCelsius; }

    private final MutableLiveData<Integer> boutonNbClick = new MutableLiveData<>();
    public LiveData<Integer> getBoutonNbClick() { return boutonNbClick; }

    //references to the Services and Characteristics of the SYM Pixl
    private BluetoothGattService timeService = null, symService = null;
    private BluetoothGattCharacteristic currentTimeChar = null, integerChar = null, temperatureChar = null, buttonClickChar = null;

    public BleOperationsViewModel(Application application) {
        super(application);
        this.mIsConnected.setValue(false); //to be sure that it's never null
        this.ble = new MySymBleManager();
        this.ble.setGattCallbacks(this.bleManagerCallbacks);
        getTemperature();
        //this.updateGui();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared");
        this.ble.disconnect();
    }

    public void connect(BluetoothDevice device) {
        Log.d(TAG, "User request connection to: " + device);
        if(!mIsConnected.getValue()) {
            this.ble.connect(device)
                    .retry(1, 100)
                    .useAutoConnect(false)
                    .enqueue();
        }
    }

    public void disconnect() {
        Log.d(TAG, "User request disconnection");
        this.ble.disconnect();
        if(mConnection != null) {
            mConnection.disconnect();
        }
    }
    public boolean readTemperature() {
        if(!isConnected().getValue() || temperatureChar == null) return false;
        return ble.readTemperature();
    }

    public boolean sendValInt(int val) {
        if(!isConnected().getValue() || integerChar == null) return false;
        return ble.sendValInt(val);
    }

    public boolean sendCurrentTime() {
        if(!isConnected().getValue() || currentTimeChar == null) return false;
        return ble.sendCurrentTime();
    }

    private BleManagerCallbacks bleManagerCallbacks = new BleManagerCallbacks() {
        @Override
        public void onDeviceConnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnecting");
            mIsConnected.setValue(false);
        }

        @Override
        public void onDeviceConnected(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnected");
            mIsConnected.setValue(true);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceDisconnecting");
            mIsConnected.setValue(false);
        }

        @Override
        public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceDisconnected");
            mIsConnected.setValue(false);
        }

        @Override
        public void onLinkLossOccurred(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onLinkLossOccurred");
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {
            Log.d(TAG, "onServicesDiscovered");
        }

        @Override
        public void onDeviceReady(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceReady");
        }

        @Override
        public void onBondingRequired(@NonNull BluetoothDevice device) {
            Log.w(TAG, "onBondingRequired");
        }

        @Override
        public void onBonded(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onBonded");
        }

        @Override
        public void onBondingFailed(@NonNull BluetoothDevice device) {
            Log.e(TAG, "onBondingFailed");
        }

        @Override
        public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
            Log.e(TAG, "onError:" + errorCode);
        }

        @Override
        public void onDeviceNotSupported(@NonNull BluetoothDevice device) {
            Log.e(TAG, "onDeviceNotSupported");
            Toast.makeText(getApplication(), "Device not supported", Toast.LENGTH_SHORT).show();
        }
    };

    /*
     *  This class is used to implement the protocol to communicate with the BLE device
     */
    private class MySymBleManager extends BleManager<BleManagerCallbacks> {

        private MySymBleManager() {
            super(getApplication());
        }

        @Override
        public BleManagerGattCallback getGattCallback() { return mGattCallback; }

        /**
         * BluetoothGatt callbacks object.
         */
        private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

            @Override
            public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
                mConnection = gatt; //trick to force disconnection
                // UUID des services souhaitées
                final String CurrentTimeService = "00001805-0000-1000-8000-00805f9b34fb";
                final String ServiceCustomSYM = "3c0a1000-281d-4b48-b2a7-f15579a1c38f";
                // UUID des caractéristiques souhaitées
                final String CurrentTimeCara = "00002a2b-0000-1000-8000-00805f9b34fb";
                final String GrapheIntCara = "3c0a1001-281d-4b48-b2a7-f15579a1c38f";
                final String TemperatureCara = "3c0a1002-281d-4b48-b2a7-f15579a1c38f";
                final String BTNCara = "3c0a1003-281d-4b48-b2a7-f15579a1c38f";

                // Savoir le nombre de service ou caractéristique trouvée
                int nbServiceFind = 0;
                int nbcaracteristiqueFind = 0;
                // Tableau des services et caractéristiques souhaitées
                final String servicesNeed[] = {
                        CurrentTimeService,
                        ServiceCustomSYM
                };

                final String caracteristiquesNeed[] = {
                        CurrentTimeCara,
                        GrapheIntCara,
                        TemperatureCara,
                        BTNCara
                };

                Log.d(TAG, "isRequiredServiceSupported - discovered services:");

                // Pour chaque service du module Bluetooth
                for (BluetoothGattService service : gatt.getServices() ) {
                    String serviceUUIDstr = service.getUuid().toString();
                    // Test si chaque service souhaité est présent
                    for (String serviceNeed : servicesNeed) {
                        if (serviceUUIDstr.equals(serviceNeed)) {
                            nbServiceFind++;
                            // Enregistre la référence au service en fonction du service
                            switch (serviceUUIDstr) {
                                case CurrentTimeService:
                                    timeService = service;
                                    break;
                                case ServiceCustomSYM:
                                    symService = service;
                                    break;
                                default:
                            }

                            // Pour chaque caractéristique du service Bluetooth
                            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                                String characteristicUUIDstr = characteristic.getUuid().toString();
                                // Test si chaque caractéristique souhaitée est présente
                                for (String caracteristique : caracteristiquesNeed) {
                                    if (characteristicUUIDstr.equals(caracteristique)) {
                                        nbcaracteristiqueFind++;
                                        // Enregistre la référence à la caractéristique
                                        switch (characteristicUUIDstr) {
                                            case CurrentTimeCara:
                                                currentTimeChar = characteristic;
                                                break;
                                            case GrapheIntCara:
                                                integerChar = characteristic;
                                                break;
                                            case TemperatureCara:
                                                temperatureChar = characteristic;
                                                break;
                                            case BTNCara:
                                                buttonClickChar = characteristic;
                                                break;
                                            default:
                                        }

                                    }
                                }
                            }
                        }
                    }
                }

                // Si tout est OK, on retourne true, sinon la librairie appelera la méthode onDeviceNotSupported()
                if (nbServiceFind == servicesNeed.length && nbcaracteristiqueFind == caracteristiquesNeed.length) {
                    return true;
                } else {
                    return false;
                }

            }

            @Override
            protected void initialize() {
                // On s'enregistre pour recevoir les notifications du temps et du nb de click
                enableNotifications(currentTimeChar).enqueue();
                enableNotifications(buttonClickChar).enqueue();


                /* Reception des notifications du temps */
                setNotificationCallback(currentTimeChar).with((device, data) -> {
                    Calendrier.setValue(convertDataToCalendar(data));
                });
                /* Reception des notif du nombre de bouton cliqués */
                setNotificationCallback(buttonClickChar).with((device, data) -> {
                    boutonNbClick.setValue(data.getIntValue(Data.FORMAT_UINT8,0));
                });

            }

            @Override
            protected void onDeviceDisconnected() {
                //we reset services and characteristics
                timeService = null;
                currentTimeChar = null;

                symService = null;
                integerChar = null;
                temperatureChar = null;
                buttonClickChar = null;
            }
        };

        /* Permet de lire la température sur le device */
        public boolean readTemperature() {
            readCharacteristic(temperatureChar).with((device, data) -> {
                temperatureCelsius.setValue(data.getIntValue(Data.FORMAT_UINT16, 0) / 10f);
            }).enqueue();

            return false; //FIXME
        }

        /* Permet d'envoyer au device un valeur en int */
        public boolean sendValInt(int value){
            // Convertie la valeur à envoyer en tableau de 4 bytes
            byte[] tabVal = new byte[]{
                    (byte)value,
                    (byte)(value >> 8),
                    (byte)(value >> 16),
                    (byte)(value >> 24)
            };

            // On envoie la valeur souhaitée
            writeCharacteristic(integerChar, tabVal).enqueue();
            return false;
        }

        /* Permet d'envoyer le temps courant */
        public boolean sendCurrentTime(){
            // On récupère et envoie le temps courant au device
            writeCharacteristic(currentTimeChar, CurrentTime()).enqueue();
            return false;
        }

        /* Permet de convertir la data recu par le device en calendrier type connu*/
        private Calendar convertDataToCalendar(Data data){

            Calendar calendrier = Calendar.getInstance();

            // On parse la data recu pour récupérer toutes les informations utiles
            calendrier.set(Calendar.YEAR, data.getIntValue(Data.FORMAT_UINT16,0));
            calendrier.set(Calendar.MONTH, data.getIntValue(Data.FORMAT_UINT8,2) - 1);
            calendrier.set(Calendar.DAY_OF_MONTH, data.getIntValue(Data.FORMAT_UINT8,3));
            calendrier.set(Calendar.HOUR_OF_DAY, data.getIntValue(Data.FORMAT_UINT8,4));
            calendrier.set(Calendar.MINUTE, data.getIntValue(Data.FORMAT_UINT8,5));
            calendrier.set(Calendar.SECOND, data.getIntValue(Data.FORMAT_UINT8,6));
            calendrier.set(Calendar.DAY_OF_WEEK, data.getIntValue(Data.FORMAT_UINT8,7));
            return calendrier;
        }

        /* Permet d'avoir le temps courant en format de tableau de byte afin de l'envoyer au device */
        private byte[] CurrentTime(){

            Calendar calendrierCurr = Calendar.getInstance();

            int year = calendrierCurr.get(Calendar.YEAR);

            byte[] BLETime = new byte[] {
                (byte) year ,
                (byte) (year >> 8),
                (byte) (calendrierCurr.get(Calendar.MONTH) + 1),
                (byte) (calendrierCurr.get(Calendar.DAY_OF_MONTH)),
                (byte) (calendrierCurr.get(Calendar.HOUR_OF_DAY)),
                (byte) (calendrierCurr.get(Calendar.MINUTE)),
                (byte) (calendrierCurr.get(Calendar.SECOND)),
                (byte) (calendrierCurr.get(Calendar.DAY_OF_WEEK)),
                /* Fraction 256 pas géré */
                (byte) (0),
                /* Adjust Reason pas géré */
                (byte) (0)
            };

            return BLETime;
        }

    }
}
